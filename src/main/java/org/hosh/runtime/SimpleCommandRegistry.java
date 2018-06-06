package org.hosh.runtime;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.State;

@NotThreadSafe
public class SimpleCommandRegistry implements CommandRegistry {

	private final State state;

	public SimpleCommandRegistry(@Nonnull State state) {
		this.state = state;
	}

	@Override
	public void registerCommand(@Nonnull String name, @Nonnull Class<? extends Command> command) {
		state.getCommands().put(name, command);
	}

}
