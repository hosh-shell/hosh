package org.hosh.modules;

import org.hosh.Command;
import org.hosh.CommandRegistry;
import org.hosh.Module;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import java.util.List;

public class TerminalModule implements Module {

    @Override
    public void beforeStart(CommandRegistry commandRegistry) {
        commandRegistry.registerCommand("clear", new Clear());
        commandRegistry.registerCommand("bell", new Bell());
    }

    @Override
    public void afterExit(CommandRegistry commandRegistry) {
        commandRegistry.unregisterCommand("clear");
    }

    static class Clear implements Command {

        @Override
        public void run(Terminal terminal, List<String> args) {
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();
        }
    }

    static class Bell implements Command {

        @Override
        public void run(Terminal terminal, List<String> args) {
            terminal.puts(InfoCmp.Capability.bell);
            terminal.flush();
        }
    }
}
