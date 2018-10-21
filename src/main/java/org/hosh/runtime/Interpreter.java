package org.hosh.runtime;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hosh.runtime.Compiler.GeneratedCommandWrapper;
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
			if (state.isExit() || exitStatus.value() != 0) {
				break;
			}
		}
		return exitStatus;
	}

	private ExitStatus execute(Statement statement) {
		Command command = statement.getCommand();
		List<String> arguments = resolveArguments(statement.getArguments());
		injectDepsIntoNested(command);
		injectDeps(command);
		return command.run(arguments, out, err);
	}

	private List<String> resolveArguments(List<String> arguments) {
		return arguments.stream().map(this::resolveVariable).collect(Collectors.toList());
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

	private void injectDepsIntoNested(Command command) {
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
