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
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;

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
			resolveArguments(statement.getArguments());
			exitStatus = execute(statement);
			store(exitStatus);
			if (userRequestedExit() || lastCommandFailed(exitStatus)) {
				break;
			}
		}
		return exitStatus;
	}

	private boolean lastCommandFailed(ExitStatus exitStatus) {
		return exitStatus.value() != 0;
	}

	private boolean userRequestedExit() {
		return state.isExit();
	}

	private void store(ExitStatus exitStatus) {
		Objects.requireNonNull(exitStatus, "exit status cannot be null");
		state.getVariables().put("EXIT_STATUS", String.valueOf(exitStatus.value()));
	}

	private ExitStatus execute(Statement statement) {
		try (Supervisor supervisor = new Supervisor(terminal)) {
			if (skipSupervision(statement)) {
				run(statement, supervisor);
			} else {
				runSupervised(statement, supervisor);
			}
			return supervisor.waitForAll(err);
		} catch (CancellationException e) {
			return ExitStatus.error();
		}
	}

	private boolean skipSupervision(Statement statement) {
		return statement.getCommand() instanceof SupervisorAware;
	}

	private void runSupervised(Statement statement, Supervisor supervisor) {
		supervisor.submit(() -> {
			supervisor.setThreadName(statement);
			return run(statement, supervisor);
		});
	}

	private ExitStatus run(Statement statement, Supervisor supervisor) {
		Command command = statement.getCommand();
		injectDeps(command, supervisor);
		List<String> resolvedArguments = resolveArguments(statement.getArguments());
		return command.run(resolvedArguments, new NullChannel(), out, err);
	}

	private void injectDeps(Command command, Supervisor supervisor) {
		command.downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
		command.downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
		command.downCast(ArgumentResolverAware.class).ifPresent(cmd -> cmd.setArgumentResolver(this::resolveArguments));
		command.downCast(SupervisorAware.class).ifPresent(cmd -> cmd.setSupervisor(supervisor));
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
}
