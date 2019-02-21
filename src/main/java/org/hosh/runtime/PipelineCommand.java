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

import java.util.List;
import java.util.logging.Logger;

import org.hosh.doc.Todo;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.runtime.PipelineChannel.ProducerPoisonPill;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;

public class PipelineCommand implements Command, TerminalAware, StateAware, ArgumentResolverAware {
	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private final Statement producer;
	private final Statement consumer;
	private ArgumentResolver argumentResolver;

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
		producer.getCommand().downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
		consumer.getCommand().downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
	}

	@Override
	public void setState(State state) {
		producer.getCommand().downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
		consumer.getCommand().downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
	}

	@Override
	public void setArgumentResolver(ArgumentResolver argumentResolver) {
		producer.getCommand().downCast(ArgumentResolverAware.class).ifPresent(cmd -> cmd.setArgumentResolver(argumentResolver));
		consumer.getCommand().downCast(ArgumentResolverAware.class).ifPresent(cmd -> cmd.setArgumentResolver(argumentResolver));
		this.argumentResolver = argumentResolver;
	}

	@Override
	public String toString() {
		return String.format("PipelineCommand[producer=%s,consumer=%s]", producer, consumer);
	}

	@Todo(description = "error channel is unbuffered by now, waiting for implementation of 2>&1")
	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		try (Supervisor supervisor = new Supervisor()) {
			supervisor.setHandleSignals(false);
			Channel pipeChannel = new PipelineChannel();
			submitProducer(supervisor, producer, new NullChannel(), pipeChannel, err);
			assemblePipeline(supervisor, consumer, pipeChannel, out, err);
			return supervisor.waitForAll(err);
		}
	}

	@Todo(description = "recursion here could be simplified?")
	private void assemblePipeline(Supervisor supervisor, Statement statement, Channel in, Channel out, Channel err) {
		if (statement.getCommand() instanceof PipelineCommand) {
			PipelineCommand pipelineCommand = (PipelineCommand) statement.getCommand();
			Channel pipelineChannel = new PipelineChannel();
			submitConsumer(supervisor, pipelineCommand.producer, in, pipelineChannel, err);
			if (pipelineCommand.consumer.getCommand() instanceof PipelineCommand) {
				assemblePipeline(supervisor, pipelineCommand.consumer, pipelineChannel, out, err);
			} else {
				submitConsumer(supervisor, pipelineCommand.consumer, pipelineChannel, out, err);
			}
		} else {
			submitConsumer(supervisor, statement, in, out, err);
		}
	}

	private void submitProducer(Supervisor supervisor, Statement statement, Channel in, Channel out, Channel err) {
		PipelineChannel pipeOut = (PipelineChannel) out;
		supervisor.submit(statement, () -> {
			Command command = statement.getCommand();
			command.downCast(ExternalCommand.class).ifPresent(ExternalCommand::pipeline);
			List<String> arguments = statement.getArguments();
			List<String> resolvedArguments = argumentResolver.resolve(arguments);
			try {
				return command.run(resolvedArguments, in, out, err);
			} catch (ProducerPoisonPill e) {
				LOGGER.finer("got poison pill");
				return ExitStatus.success();
			} finally {
				pipeOut.stopConsumer();
			}
		});
	}

	private void submitConsumer(Supervisor supervisor, Statement statement, Channel in, Channel out, Channel err) {
		PipelineChannel pipeIn = (PipelineChannel) in;
		supervisor.submit(statement, () -> {
			Command command = statement.getCommand();
			command.downCast(ExternalCommand.class).ifPresent(ExternalCommand::pipeline);
			List<String> arguments = statement.getArguments();
			List<String> resolvedArguments = argumentResolver.resolve(arguments);
			try {
				return command.run(resolvedArguments, in, out, err);
			} catch (ProducerPoisonPill e) {
				LOGGER.finer("got poison pill");
				return ExitStatus.success();
			} finally {
				pipeIn.stopProducer();
				pipeIn.consumeAnyRemainingRecord();
				if (out instanceof PipelineChannel) {
					PipelineChannel pipeOut = (PipelineChannel) out;
					pipeOut.stopConsumer();
				}
			}
		});
	}
}
