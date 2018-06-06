package org.hosh.runtime;

import javax.annotation.Nonnull;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;

public class CommandFactory {

	private final Terminal terminal;
	private final State state;

	public CommandFactory(@Nonnull State state, @Nonnull Terminal terminal) {
		this.state = state;
		this.terminal = terminal;
	}

	public Command create(Class<? extends Command> commandClass) {
		Command command = createCommand(commandClass);
		handleTerminalAware(command);
		handleStateAware(command);
		return command;
	}

	private void handleStateAware(Command command) {
		if (command instanceof StateAware) {
			((StateAware) command).setState(state);
		}
	}

	private void handleTerminalAware(Command command) {
		if (command instanceof TerminalAware) {
			((TerminalAware) command).setTerminal(terminal);
		}
	}

	private Command createCommand(Class<? extends Command> commandClass) {
		try {
			return commandClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalArgumentException("cannot instantiate command using default empty costructor", ex);
		}
	}
}
