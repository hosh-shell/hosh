package org.hosh;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NotThreadSafe
public class SimpleCommandRegistry implements CommandRegistry {

    private final Map<String, Class<? extends Command>> commandsByName = new HashMap<>();
    private final CommandFactory commandFactory;

    public SimpleCommandRegistry(@Nonnull CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @Override
    public void registerCommand(@Nonnull String name, Class<? extends Command> command) {
        commandsByName.put(name, command);
    }

    @Override
    public Optional<Command> search(String name) {
        Class<? extends Command> commandClass = commandsByName.get(name);
        return Optional.ofNullable(commandClass).map(commandFactory::create);
    }

    @Override
    public Collection<String> commandNames() {
        return commandsByName.keySet();
    }
}
