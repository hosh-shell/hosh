/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package hosh.runtime;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import hosh.runtime.Compiler.Statement;
import hosh.runtime.PipelineChannel.ProducerPoisonPill;
import hosh.spi.Channel;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.LoggerFactory;

public class PipelineCommand implements Command, InterpreterAware {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private final Statement producer;

	private final Statement consumer;

	private Interpreter interpreter;

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
	public String toString() {
		return String.format("PipelineCommand[producer=%s,consumer=%s]", producer, consumer);
	}

	@Override
	public void setInterpreter(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	// Used to control to control pipe of an external command.
	// Inspired by implementation of
	// java.lang.ProcessBuilder.startPipeline(List<ProcessBuilder).
	public enum Position {
			// command not in a pipeline
			SOLE(false, false),
			// first command of a pipeline
			FIRST(false, true),
			// last command of a pipeline
			LAST(true, false),
			// everything else
			MIDDLE(false, false);

		private final boolean redirectInput;

		private final boolean redirectOutput;

		Position(boolean redirectInput, boolean redirectOutput) {
			this.redirectInput = redirectInput;
			this.redirectOutput = redirectOutput;
		}

		public boolean redirectInput() {
			return redirectInput;
		}

		public boolean redirectOutput() {
			return redirectOutput;
		}
	}

	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		try (Supervisor supervisor = new Supervisor()) {
			supervisor.setHandleSignals(false);
			Channel pipeChannel = new PipelineChannel();
			runAsync(supervisor, producer, in, pipeChannel, err, Position.FIRST);
			assemblePipeline(supervisor, consumer, pipeChannel, out, err);
			return supervisor.waitForAll();
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private void assemblePipeline(Supervisor supervisor, Statement statement, Channel in, Channel out, Channel err) {
		if (statement.getCommand() instanceof PipelineCommand) {
			PipelineCommand pipelineCommand = (PipelineCommand) statement.getCommand();
			Channel pipelineChannel = new PipelineChannel();
			runAsync(supervisor, pipelineCommand.producer, in, pipelineChannel, err, Position.MIDDLE);
			assemblePipeline(supervisor, pipelineCommand.consumer, pipelineChannel, out, err);
		} else {
			runAsync(supervisor, statement, in, out, err, Position.LAST);
		}
	}

	private void runAsync(Supervisor supervisor, Statement statement, Channel in, Channel out, Channel err, Position position) {
		supervisor.submit(() -> {
			Command command = statement.getCommand();
			Downcast.of(command, ExternalCommand.class).ifPresent(cmd -> cmd.pipeline(position));
			try {
				return interpreter.run(statement, in, out, err);
			} catch (ProducerPoisonPill e) {
				LOGGER.finer(() -> String.format("got poison pill from command '%s'", statement.getLocation()));
				return ExitStatus.success();
			} finally {
				stopProducer(in);
				stopConsumer(out);
			}
		});
	}

	private void stopConsumer(Channel out) {
		Downcast.of(out, PipelineChannel.class).ifPresent(pipeOut -> {
			pipeOut.stopConsumer();
		});
	}

	private void stopProducer(Channel in) {
		Downcast.of(in, PipelineChannel.class).ifPresent(pipeIn -> {
			pipeIn.stopProducer();
			pipeIn.consumeAnyRemainingRecord();
		});
	}
}