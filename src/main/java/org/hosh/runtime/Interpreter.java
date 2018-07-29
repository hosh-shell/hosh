package org.hosh.runtime;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hosh.doc.Todo;
import org.hosh.runtime.Compiler.GeneratedCommand;
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

	@Todo(description = "exit at first error")
	public void eval(Program program) {
		for (Statement statement : program.getStatements()) {
			ExitStatus exitStatus = execute(statement);
			store(exitStatus);
		}
	}

	private ExitStatus execute(Statement statement) {
		Command command = statement.getCommand();
		List<String> arguments = statement.getArguments();
		injectDepsIntoNested(command);
		injectDeps(command);
		return command.run(arguments, out, err);
	}

	private void store(ExitStatus exitStatus) {
		Objects.requireNonNull(exitStatus, "exit status cannot be null");
		state.getVariables().put("EXIT_STATUS", String.valueOf(exitStatus.value()));
	}

	@Todo(description = "this is another design error? we need a way a tree of commands")
	private void injectDepsIntoNested(Command command) {
		if (command instanceof GeneratedCommand) {
			injectDeps(((GeneratedCommand) command).getNestedStatement().getCommand());
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
