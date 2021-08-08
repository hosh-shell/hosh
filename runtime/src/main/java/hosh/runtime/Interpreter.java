/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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

import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Interpreter {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private final State state;
	private final Injector injector;

	public Interpreter(State state, Injector injector) {
		this.state = state;
		this.injector = injector;
	}

	public ExitStatus eval(Compiler.Program program, OutputChannel out, OutputChannel err) {
		ExitStatus exitStatus = ExitStatus.success();
		for (Compiler.Statement statement : program.getStatements()) {
			exitStatus = evalUnderSupervision(statement, out, err);
			store(exitStatus);
			if (userRequestedExit() || lastCommandFailed(exitStatus)) {
				break;
			}
		}
		return exitStatus;
	}

	private boolean lastCommandFailed(ExitStatus exitStatus) {
		return exitStatus.isError();
	}

	private boolean userRequestedExit() {
		return state.isExit();
	}

	private void store(ExitStatus exitStatus) {
		Objects.requireNonNull(exitStatus, "exit status cannot be null");
		state.getVariables().put("EXIT_STATUS", String.valueOf(exitStatus.value()));
	}

	private ExitStatus evalUnderSupervision(Compiler.Statement statement, OutputChannel out, OutputChannel err) {
		try (Supervisor supervisor = new Supervisor()) {
			supervisor.submit(() -> eval(statement, new NullChannel(), out, err));
			return supervisor.waitForAll();
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "caught exception", e);
			Record record = Records.builder()
				                .entry(Keys.LOCATION, Values.ofText(statement.getLocation()))
				                .entry(Keys.ERROR, Values.ofText(messageFor(e)))
				                .build();
			err.send(record);
			return ExitStatus.error();
		}
	}

	protected ExitStatus eval(Compiler.Statement statement, InputChannel in, OutputChannel out, OutputChannel err) {
		Command command = statement.getCommand();
		injectInterpreter(command);
		injector.injectDeps(command);
		List<String> resolvedArguments = resolveArguments(statement.getArguments());
		changeCurrentThreadName(statement.getLocation(), resolvedArguments);
		return command.run(resolvedArguments, in, out, new WithLocation(err, statement.getLocation()));
	}

	private void injectInterpreter(Command command) {
		if (command instanceof InterpreterAware) {
			((InterpreterAware) command).setInterpreter(this);
		}
	}

	private void changeCurrentThreadName(String commandName, List<String> resolvedArguments) {
		List<String> commandWithArguments = new ArrayList<>();
		commandWithArguments.add(commandName);
		commandWithArguments.addAll(resolvedArguments);
		String name = String.format("command='%s'", String.join(" ", commandWithArguments));
		Thread.currentThread().setName(name);
	}

	private List<String> resolveArguments(List<Compiler.Resolvable> arguments) {
		return arguments
			       .stream()
			       .map(resolvable -> resolvable.resolve(state))
			       .collect(Collectors.toList());
	}

	private String messageFor(ExecutionException e) {
		if (e.getCause() != null && e.getCause().getMessage() != null) {
			return e.getCause().getMessage();
		} else {
			return "(no message provided)";
		}
	}

	// enrich any record sent to the inner channel
	// with location of the current statement
	private static class WithLocation implements OutputChannel {

		private final OutputChannel channel;

		private final String location;

		public WithLocation(OutputChannel channel, String location) {
			this.channel = channel;
			this.location = location;
		}

		@Override
		public void send(Record record) {
			channel.send(record.prepend(Keys.LOCATION, Values.ofText(location)));
		}
	}
}
