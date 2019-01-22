package org.hosh.runtime;

import java.util.Map;
import java.util.Objects;

import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.State;

public class SimpleCommandRegistry implements CommandRegistry {
	private final State state;

	public SimpleCommandRegistry(State state) {
		this.state = state;
	}

	@Override
	public void registerCommand(String name, Command command) {
		Objects.requireNonNull(name, "name cannot be null");
		Objects.requireNonNull(command, "command cannot be null");
		Map<String, Command> commands = state.getCommands();
		if (commands.containsKey(name)) {
			throw new IllegalArgumentException("command with same name already registered: " + name);
		}
		commands.putIfAbsent(name, command);
	}
}
