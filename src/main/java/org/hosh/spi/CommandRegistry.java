package org.hosh.spi;

import javax.annotation.Nonnull;

public interface CommandRegistry {

	void registerCommand(@Nonnull String name, @Nonnull Class<? extends Command> command);

}
