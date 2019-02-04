package org.hosh.runtime;

import java.util.List;
import java.util.Objects;
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

	private ExitStatus execute(Statement statement) {
		Command command = prepareCommand(statement);
		List<String> arguments = resolveArguments(statement.getArguments());
		return command.run(arguments, new NullChannel(), out, err);
	}

	private Command prepareCommand(Statement statement) {
		Command command = statement.getCommand();
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

	private void injectDeps(Command command) {
		command.downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
		command.downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
	}
}
