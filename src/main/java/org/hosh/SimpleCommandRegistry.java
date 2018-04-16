package org.hosh;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SimpleCommandRegistry implements CommandRegistry {

    private final Map<String, Command> commandsByName = new HashMap<>();

    @Override
    public void registerCommand(String name, Command command) {
        commandsByName.put(name, command);
    }

    @Override
    public void unregisterCommand(String name) {
        commandsByName.remove(name);
    }

    @Override
    public Optional<Command> search(String name) {
        return Optional.ofNullable(commandsByName.get(name));
    }
}
