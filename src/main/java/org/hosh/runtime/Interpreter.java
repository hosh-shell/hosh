/**
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
package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Resolvable;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.LineReaderAware;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Records;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.hosh.spi.Values;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;

public class Interpreter {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private final State state;

	private final Terminal terminal;

	// this is a "private" LineReader to be injected in commands: it has no history
	// and no auto-complete
	private final LineReader lineReader;

	private final Channel out;

	private final Channel err;

	public Interpreter(State state, Terminal terminal, Channel out, Channel err) {
		this.state = state;
		this.terminal = terminal;
		this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
		this.out = out;
		this.err = err;
	}

	public ExitStatus eval(Program program) {
		ExitStatus exitStatus = ExitStatus.success();
		for (Statement statement : program.getStatements()) {
			exitStatus = execute(statement);
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

	private ExitStatus execute(Statement statement) {
		try (Supervisor supervisor = new Supervisor()) {
			runSupervised(statement, supervisor);
			return supervisor.waitForAll();
		} catch (CancellationException e) {
			LOGGER.log(Level.INFO, "got cancellation", e);
			return ExitStatus.error();
		} catch (InterruptedException e) {
			LOGGER.log(Level.INFO, "got interrupt", e);
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "caught exception", e);
			String message = messageFor(e);
			err.send(Records.singleton(Keys.ERROR, Values.ofText(message)));
			return ExitStatus.error();
		}
	}

	private String messageFor(ExecutionException e) {
		if (e.getCause() != null && e.getCause().getMessage() != null) {
			return e.getCause().getMessage();
		} else {
			return "(no message provided)";
		}
	}

	private void runSupervised(Statement statement, Supervisor supervisor) {
		supervisor.submit(() -> run(statement));
	}

	private ExitStatus run(Statement statement) throws Exception {
		return run(statement, new NullChannel(), out, err);
	}

	public ExitStatus run(Statement statement, Channel in, Channel out, Channel err) throws Exception {
		Command command = statement.getCommand();
		injectDeps(command);
		List<String> resolvedArguments = resolveArguments(statement.getArguments());
		changeCurrentThreadName(command.describe(), resolvedArguments);
		return command.run(resolvedArguments, in, out, err);
	}

	private void changeCurrentThreadName(String commandName, List<String> resolvedArguments) {
		List<String> commandWithArguments = new ArrayList<>();
		commandWithArguments.add(commandName);
		commandWithArguments.addAll(resolvedArguments);
		String name = String.format("command='%s'", String.join(" ", commandWithArguments));
		Thread.currentThread().setName(name);
	}

	private void injectDeps(Command command) {
		command.downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
		command.downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
		command.downCast(LineReaderAware.class).ifPresent(cmd -> cmd.setLineReader(lineReader));
		command.downCast(InterpreterAware.class).ifPresent(cmd -> cmd.setInterpreter(this));
	}

	private List<String> resolveArguments(List<Resolvable> arguments) {
		return arguments
				.stream()
				.map(resolvable -> resolvable.resolve(state))
				.collect(Collectors.toList());
	}
}
