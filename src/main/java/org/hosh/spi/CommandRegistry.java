package org.hosh.spi;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface CommandRegistry {

	// TODO: used only by modules to register commands
    void registerCommand(@Nonnull String name, @Nonnull Class<? extends Command> command);

    // TODO: used only by interactive shell to "compile" commands
    Optional<Command> search(@Nonnull String name);

    // TODO: used only by command completer
    Collection<String> commandNames();
}
