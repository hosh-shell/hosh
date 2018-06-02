package org.hosh.runtime;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;

public class CommandFactory {

	private final Terminal terminal;
	private final State state;
	
	public CommandFactory(Terminal terminal, State state) {
		this.terminal = terminal;
		this.state = state;
	}

	public Command create(Class<? extends Command> commandClass) {
		Command command = createCommand(commandClass);
		if (command instanceof TerminalAware) {
			((TerminalAware) command).setTerminal(terminal);
		}
		if (command instanceof StateAware) {
			((StateAware) command).setState(state);
		}
		
		return command;
	}

	private Command createCommand(Class<? extends Command> commandClass) {
		try {
			return commandClass.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalArgumentException("cannot instantiate command using default empty costructor", ex);
		}
	}
}
