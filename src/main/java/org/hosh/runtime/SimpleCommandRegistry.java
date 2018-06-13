package org.hosh.runtime;

import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.State;

public class SimpleCommandRegistry implements CommandRegistry {

	private final State state;

	public SimpleCommandRegistry(State state) {
		this.state = state;
	}

	@Override
	public void registerCommand(String name, Class<? extends Command> command) {
		state.getCommands().put(name, command);
	}

}
