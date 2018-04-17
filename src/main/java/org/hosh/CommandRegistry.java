package org.hosh;

import java.util.Collection;
import java.util.Optional;

public interface CommandRegistry {

    void registerCommand(String name, Class<? extends Command> command);

    Optional<Command> search(String name);

    Collection<String> commandNames();
}
