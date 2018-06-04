package org.hosh.spi;

import javax.annotation.Nonnull;

/** Modules uses this to register command classes */
public interface CommandRegistry {

	void registerCommand(@Nonnull String name, @Nonnull Class<? extends Command> command);

}
