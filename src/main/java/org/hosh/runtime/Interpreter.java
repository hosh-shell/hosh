package org.hosh.runtime;

import java.util.List;
import java.util.Optional;

import org.hosh.runtime.Compiler.GeneratedCommand;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
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

	public void eval(Program program) {
		for (Statement statement : program.getStatements()) {
			Command command = statement.getCommand();
			if (command instanceof GeneratedCommand) {
				injectDeps(((GeneratedCommand) command).getNestedStatement().getCommand());
			}
			List<String> arguments = statement.getArguments();
			injectDeps(command);
			command.run(arguments, out, err);
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
