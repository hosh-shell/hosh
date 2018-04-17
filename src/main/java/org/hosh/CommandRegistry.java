package org.hosh;

import java.util.Optional;

public interface CommandRegistry {

    void registerCommand(String name, Class<? extends Command> command);

    void unregisterCommand(String name);

    Optional<Command> search(String name);

}
