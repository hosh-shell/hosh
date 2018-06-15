package org.hosh.runtime;

import java.util.Objects;

import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.State;

public class SimpleCommandRegistry implements CommandRegistry {

	private final State state;

	public SimpleCommandRegistry(State state) {
		this.state = state;
	}

	// TODO: check for overwriting existing commands
	@Override
	public void registerCommand(String name, Command command) {
		Objects.requireNonNull(name, "name cannot be null");
		Objects.requireNonNull(command, "command cannot be null");
		state.getCommands().put(name, command);
	}

}
