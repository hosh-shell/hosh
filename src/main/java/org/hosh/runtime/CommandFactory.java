package org.hosh.runtime;

import org.hosh.spi.Command;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;

public class CommandFactory {

    private final Terminal terminal;

    public CommandFactory(Terminal terminal) {
        this.terminal = terminal;
    }

    public Command create(Class<? extends Command> commandClass) {
        Command command = createCommand(commandClass);
        if (command instanceof TerminalAware) {
            ((TerminalAware)command).setTerminal(terminal);
        }
        return command;
    }

    private Command createCommand(Class<? extends Command> commandClass) {
        try {
            return commandClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("cannot instantiate command using default empty costructor", ex);
        }
    }
}
