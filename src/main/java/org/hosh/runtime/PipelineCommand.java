package org.hosh.runtime;

import java.util.List;
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
import org.hosh.spi.Values;
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
		PipeChannel pipeChannel = new PipeChannel(queue);
		Future<ExitStatus> producer = prepareProducer(new UnlinkedChannel(), pipeChannel, err, executor);
		Future<ExitStatus> consumer = prepareConsumer(pipeChannel, out, err, executor);
		try {
			return run(producer, consumer, err);
		} finally {
			executor.shutdownNow();
		}
	}

	private ExitStatus run(Future<ExitStatus> producer, Future<ExitStatus> consumer, Channel err) {
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
			String details;
			if (e.getCause() != null && e.getCause().getMessage() != null) {
				details = e.getCause().getMessage();
			} else {
				details = "";
			}
			err.send(Record.of("error", Values.ofText(details)));
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

	private Future<ExitStatus> prepareProducer(Channel in, PipeChannel out, Channel err, ExecutorService executor) {
		return executor.submit(() -> {
			setThreadName(statements.get(0));
			List<String> arguments = statements.get(0).getArguments();
			Command command = statements.get(0).getCommand();
			command.downCast(ExternalCommand.class).ifPresent(cmd -> cmd.pipeline());
			ExitStatus st = command.run(arguments, in, out, err);
			out.stopConsumer();
			return st;
		});
	}

	private Future<ExitStatus> prepareConsumer(PipeChannel in, Channel out, Channel err, ExecutorService executor) {
		return executor.submit(() -> {
			setThreadName(statements.get(1));
			Command command = statements.get(1).getCommand();
			command.downCast(ExternalCommand.class).ifPresent(cmd -> cmd.pipeline());
			List<String> arguments = statements.get(1).getArguments();
			try {
				return command.run(arguments, in, out, err);
			} finally {
				in.consumeAnyRemainingRecord();
			}
		});
	}

	private void setThreadName(Statement statement) {
		Thread.currentThread().setName(String.format("Pipeline/command='%s %s'",
				statement.getCommand().getClass().getSimpleName(),
				String.join(" ", statement.getArguments())));
	}
}
