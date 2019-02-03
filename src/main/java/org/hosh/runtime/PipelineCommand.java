package org.hosh.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.hosh.doc.Todo;
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

public class PipelineCommand implements Command, TerminalAware, StateAware {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final List<Statement> statements;
	private Terminal terminal;

	public PipelineCommand(List<Statement> statements) {
		this.statements = statements;
	}

	@Override
	public void setTerminal(Terminal terminal) {
		this.terminal = terminal;
		for (Statement statement : statements) {
			statement.getCommand().downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
		}
	}

	@Override
	public void setState(State state) {
		for (Statement statement : statements) {
			statement.getCommand().downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
		}
	}

	@Todo(description = "error channel is unbuffered by now, waiting for implementation of 2>&1")
	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		BlockingQueue<Record> queue = new LinkedBlockingQueue<>(10);
		Channel pipeChannel = new QueueingChannel(queue);
		Future<ExitStatus> producer = prepareProducer(new UnlinkedChannel(), pipeChannel, err, executor, queue);
		Future<ExitStatus> consumer = prepareConsumer(pipeChannel, out, err, executor);
		try {
			return run(producer, consumer);
		} finally {
			executor.shutdownNow();
		}
	}

	private ExitStatus run(Future<ExitStatus> producer, Future<ExitStatus> consumer) {
		terminal.handle(Signal.INT, signal -> {
			cancelIfStillRunning(producer);
			cancelIfStillRunning(consumer);
		});
		try {
			ExitStatus producerExitStatus = producer.get();
			ExitStatus consumerExitStatus = consumer.get();
			if (producerExitStatus.isSuccess() && consumerExitStatus.isSuccess()) {
				return ExitStatus.success();
			} else {
				return ExitStatus.error();
			}
		} catch (CancellationException e) {
			return ExitStatus.error();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			logger.error("caught exception", e);
			return ExitStatus.error();
		} finally {
			terminal.handle(Signal.INT, SignalHandler.SIG_DFL);
		}
	}

	private void cancelIfStillRunning(Future<ExitStatus> future) {
		if (!future.isCancelled() && !future.isDone()) {
			future.cancel(true);
		}
	}

	private Future<ExitStatus> prepareProducer(Channel in, Channel out, Channel err, ExecutorService executor, BlockingQueue<Record> queue) {
		return executor.submit(() -> {
			setThreadName(statements.get(0));
			List<String> arguments = statements.get(0).getArguments();
			Command command = statements.get(0).getCommand();
			command.downCast(ExternalCommand.class).ifPresent(cmd -> cmd.pipeline());
			ExitStatus st = command.run(arguments, in, out, err);
			queue.put(QueueingChannel.POISON_PILL);
			return st;
		});
	}

	private Future<ExitStatus> prepareConsumer(Channel in, Channel out, Channel err, ExecutorService executor) {
		return executor.submit(() -> {
			setThreadName(statements.get(1));
			Command command = statements.get(1).getCommand();
			command.downCast(ExternalCommand.class).ifPresent(cmd -> cmd.pipeline());
			List<String> arguments = statements.get(1).getArguments();
			try {
				return command.run(arguments, in, out, err);
			} finally {
				consumeAnyRemainingRecord(in);
			}
		});
	}

	// let the producer to stop, otherwise it could be blocked
	// during put() in the queue
	private void consumeAnyRemainingRecord(Channel in) {
		logger.trace("consuming remaining records");
		while (true) {
			Optional<Record> incoming = in.recv();
			if (incoming.isEmpty()) {
				break;
			}
			logger.trace("  discarding {}", incoming.get());
		}
		logger.trace("consumed remaining items");
	}

	private void setThreadName(Statement statement) {
		Thread.currentThread().setName(String.format("Pipeline/command='%s %s'",
				statement.getCommand().getClass().getSimpleName(),
				String.join(" ", statement.getArguments())));
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
				if (done) {
					return Optional.empty();
				}
				logger.trace("waiting for record... ");
				Record record = queue.take();
				if (POISON_PILL.equals(record)) {
					logger.trace("got POISON_PILL... ");
					done = true;
					return Optional.empty();
				}
				logger.trace("got record {}", record);
				return Optional.ofNullable(record);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Optional.empty();
			}
		}

		@Override
		public void send(Record record) {
			logger.trace("sending record {}", record);
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
				logger.trace("record {} not sent", record);
				return true;
			} else {
				send(record);
				logger.trace("record {} sent", record);
				return false;
			}
		}
	}
}
