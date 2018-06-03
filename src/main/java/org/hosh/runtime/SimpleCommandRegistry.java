package org.hosh.runtime;

import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.State;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
