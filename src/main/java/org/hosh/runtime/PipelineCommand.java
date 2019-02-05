/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hosh.doc.Todo;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.runtime.PipelineChannel.ProducerPoisonPill;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(PipelineCommand.class);
	private final Statement producer;
	private final Statement consumer;
	private Terminal terminal;

	public PipelineCommand(Statement producer, Statement consumer) {
		this.producer = producer;
		this.consumer = consumer;
	}

	public Statement getProducer() {
		return producer;
	}

	public Statement getConsumer() {
		return consumer;
	}

	@Override
	public void setTerminal(Terminal terminal) {
		this.terminal = terminal;
		producer.getCommand().downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
		consumer.getCommand().downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
	}

	@Override
	public void setState(State state) {
		producer.getCommand().downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
		consumer.getCommand().downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
	}

	@Override
	public String toString() {
		return String.format("PipelineCommand[producer=%s,consumer=%s]", producer, consumer);
	}

	@Todo(description = "error channel is unbuffered by now, waiting for implementation of 2>&1")
	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<ExitStatus>> futures = new ArrayList<>();
		Channel pipeChannel = new PipelineChannel();
		Future<ExitStatus> producerFuture = submitProducer(producer, new NullChannel(), pipeChannel, err, executor);
		futures.add(producerFuture);
		assemblePipeline(futures, consumer, pipeChannel, out, err, executor);
		try {
			return run(futures, err);
		} finally {
			executor.shutdownNow();
		}
	}

	private ExitStatus run(List<Future<ExitStatus>> futures, Channel err) {
		terminal.handle(Signal.INT, signal -> {
			futures.forEach(this::cancelIfStillRunning);
		});
		try {
			List<ExitStatus> results = new ArrayList<>();
			for (Future<ExitStatus> future : futures) {
				results.add(future.get());
			}
			boolean allSuccesses = results.stream().allMatch(ExitStatus::isSuccess);
			return allSuccesses ? ExitStatus.success() : ExitStatus.error();
		} catch (CancellationException e) {
			return ExitStatus.error();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			LOGGER.error("caught exception", e);
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

	private void assemblePipeline(List<Future<ExitStatus>> futures,
			Statement statement,
			Channel in, Channel out, Channel err,
			ExecutorService executor) {
		if (statement.getCommand() instanceof PipelineCommand) {
			PipelineCommand pipelineCommand = (PipelineCommand) statement.getCommand();
			Channel pipelineChannel = new PipelineChannel();
			Future<ExitStatus> producerFuture = submitConsumer(pipelineCommand.producer, in, pipelineChannel, err, executor);
			futures.add(producerFuture);
			if (pipelineCommand.consumer.getCommand() instanceof PipelineCommand) {
				assemblePipeline(futures, pipelineCommand.consumer, pipelineChannel, out, err, executor);
			} else {
				Future<ExitStatus> consumerFuture = submitConsumer(pipelineCommand.consumer, pipelineChannel, out, err, executor);
				futures.add(consumerFuture);
			}
		} else {
			Future<ExitStatus> consumerFuture = submitConsumer(statement, in, out, err, executor);
			futures.add(consumerFuture);
		}
	}

	private Future<ExitStatus> submitProducer(Statement statement, Channel in, Channel out, Channel err, ExecutorService executor) {
		return executor.submit(new Callable<>() {
			@Override
			public ExitStatus call() throws Exception {
				setThreadName(statement);
				List<String> arguments = statement.getArguments();
				Command command = statement.getCommand();
				command.downCast(ExternalCommand.class).ifPresent(ExternalCommand::pipeline);
				try {
					return command.run(arguments, in, out, err);
				} catch (ProducerPoisonPill e) {
					LOGGER.trace("got poison pill");
					return ExitStatus.success();
				} finally {
					((PipelineChannel) out).stopConsumer();
				}
			}

			@Override
			public String toString() {
				return statement.toString();
			}
		});
	}

	private Future<ExitStatus> submitConsumer(Statement statement, Channel in, Channel out, Channel err, ExecutorService executor) {
		return executor.submit(new Callable<>() {
			@Override
			public ExitStatus call() throws Exception {
				setThreadName(statement);
				Command command = statement.getCommand();
				command.downCast(ExternalCommand.class).ifPresent(ExternalCommand::pipeline);
				List<String> arguments = statement.getArguments();
				try {
					return command.run(arguments, in, out, err);
				} finally {
					((PipelineChannel) in).stopProducer();
					((PipelineChannel) in).consumeAnyRemainingRecord();
					if (out instanceof PipelineChannel) {
						((PipelineChannel) out).stopConsumer();
					}
				}
			}

			@Override
			public String toString() {
				return statement.toString();
			}
		});
	}

	private void setThreadName(Statement statement) {
		Thread.currentThread().setName(String.format("Pipeline/command='%s %s'",
				statement.getCommand().getClass().getSimpleName(),
				String.join(" ", statement.getArguments())));
	}
}
