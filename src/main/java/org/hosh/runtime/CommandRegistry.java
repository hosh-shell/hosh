package org.hosh.runtime;

import org.hosh.spi.Command;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface CommandRegistry {

    void registerCommand(@Nonnull String name, @Nonnull Class<? extends Command> command);

    Optional<Command> search(@Nonnull String name);

    Collection<String> commandNames();
}
