package org.hosh.runtime;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.hosh.doc.Todo;
import org.hosh.runtime.Compiler.GeneratedCommandWrapper;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Interpreter {
	private final State state;
	private final Terminal terminal;
	private final Channel out;
	private final Channel err;

	public Interpreter(State state, Terminal terminal, Channel out, Channel err) {
		this.state = state;
		this.terminal = terminal;
		this.out = out;
		this.err = err;
	}

	public ExitStatus eval(Program program) {
		ExitStatus exitStatus = ExitStatus.success();
		for (Statement statement : program.getStatements()) {
			exitStatus = execute(statement);
			store(exitStatus);
			if (state.isExit() || exitStatus.value() != 0) {
				break;
			}
		}
		return exitStatus;
	}

	private ExitStatus execute(Statement statement) {
		if (statement.getNext() == null) {
			return runStandaloneStatement(statement);
		} else {
			return runPipelinedStatement(statement);
		}
	}

	private ExitStatus runStandaloneStatement(Statement statement) {
		Command command = prepareCommand(statement);
		List<String> arguments = resolveArguments(statement.getArguments());
		return command.run(arguments, new UnlinkedChannel(), out, err);
	}

	@Todo(description = "move as tunable parameter? via state or env var")
	private static final int QUEUE_CAPACITY = 5;

	@Todo(description = "error channel is unbuffered by now, waiting for implementation of 2>&1")
	private ExitStatus runPipelinedStatement(Statement statement) {
		BlockingQueue<Record> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
		Channel pipeChannel = new QueueingChannel(queue);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Command command = prepareCommand(statement);
		List<String> arguments = resolveArguments(statement.getArguments());
		Future<ExitStatus> producer = executor.submit(() -> {
			ExitStatus st = command.run(arguments, new UnlinkedChannel(), pipeChannel, err);
			queue.put(QueueingChannel.POISON_PILL);
			return st;
		});
		Command nextCommand = prepareCommand(statement.getNext());
		List<String> nextArguments = resolveArguments(statement.getNext().getArguments());
		Future<ExitStatus> consumer = executor.submit(() -> nextCommand.run(nextArguments, pipeChannel, out, err));
		terminal.handle(Signal.INT, signal -> {
			producer.cancel(true);
			consumer.cancel(true);
		});
		try {
			ExitStatus producerExitStatus = producer.get();
			ExitStatus consumerExitStatus = consumer.get();
			if (producerExitStatus.isSuccess() && consumerExitStatus.isSuccess()) {
				return ExitStatus.success();
			} else {
				return ExitStatus.error();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			return ExitStatus.error();
		} finally {
			executor.shutdownNow();
			terminal.handle(Signal.INT, SignalHandler.SIG_DFL);
		}
	}

	private static class UnlinkedChannel implements Channel {
		@Override
		public Optional<Record> recv() {
			throw new UnsupportedOperationException("cannot read from this channel");
		}

		@Override
		public void send(Record record) {
			throw new UnsupportedOperationException("cannot write to this channel");
		}
	}

	@Todo(description = "improve InterruptedException handling")
	private static class QueueingChannel implements Channel {
		public static final Record POISON_PILL = Record.of("__POISON_PILL__", null);
		private final Logger logger = LoggerFactory.getLogger(getClass());
		private final BlockingQueue<Record> queue;
		private volatile boolean done = false;

		public QueueingChannel(BlockingQueue<Record> queue) {
			this.queue = queue;
		}

		@Override
		public Optional<Record> recv() {
			try {
				logger.debug("waiting for record... ");
				Record record = queue.take();
				if (POISON_PILL.equals(record)) {
					logger.debug("got POISON_PILL... ");
					return Optional.empty();
				}
				logger.debug("got record {}", record);
				return Optional.ofNullable(record);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Optional.empty();
			}
		}

		@Override
		public void send(Record record) {
			logger.debug("sent record {}", record);
			try {
				queue.put(record);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void requestStop() {
			done = true;
			logger.debug("stop requested, done={}", done);
		}

		@Override
		public boolean trySend(Record record) {
			if (done) {
				logger.debug("record {} *not sent downstream", record);
				return true;
			} else {
				send(record);
				logger.debug("record {} sent downstream", record);
				return false;
			}
		}
	}

	private Command prepareCommand(Statement statement) {
		Command command = statement.getCommand();
		injectDepsIntoWrapper(command);
		injectDeps(command);
		return command;
	}

	private List<String> resolveArguments(List<String> arguments) {
		return arguments
				.stream()
				.map(this::resolveVariable)
				.collect(Collectors.toList());
	}

	private String resolveVariable(String argument) {
		if (argument.startsWith("${") && argument.endsWith("}")) {
			String variableName = variableName(argument);
			if (state.getVariables().containsKey(variableName)) {
				return state.getVariables().get(variableName);
			} else {
				throw new IllegalStateException("unknown variable: " + variableName);
			}
		} else {
			return argument;
		}
	}

	// ${VARIABLE} -> VARIABLE
	private String variableName(String variable) {
		return variable.substring(2, variable.length() - 1);
	}

	private void store(ExitStatus exitStatus) {
		Objects.requireNonNull(exitStatus, "exit status cannot be null");
		state.getVariables().put("EXIT_STATUS", String.valueOf(exitStatus.value()));
	}

	private void injectDepsIntoWrapper(Command command) {
		if (command instanceof GeneratedCommandWrapper) {
			injectDeps(((GeneratedCommandWrapper) command).getNestedStatement().getCommand());
		}
	}

	private void injectDeps(Command command) {
		downCast(command, StateAware.class).ifPresent(cmd -> cmd.setState(state));
		downCast(command, TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
	}

	private static <T> Optional<T> downCast(Object object, Class<T> requiredClass) {
		if (requiredClass.isInstance(object)) {
			return Optional.of(requiredClass.cast(object));
		} else {
			return Optional.empty();
		}
	}
}
