package org.hosh.spi;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface CommandRegistry {

	void registerCommand(@Nonnull String name, @Nonnull Class<? extends Command> command);

}
