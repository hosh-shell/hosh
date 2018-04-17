package org.hosh.modules;

import org.hosh.Command;
import org.hosh.CommandRegistry;
import org.hosh.Module;
import org.hosh.TerminalAware;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import java.util.List;

public class TerminalModule implements Module {

    @Override
    public void onStartup(CommandRegistry commandRegistry) {
        commandRegistry.registerCommand("clear", Clear.class);
        commandRegistry.registerCommand("bell", Bell.class);
    }

    public static class Clear implements Command, TerminalAware {

        private Terminal terminal;

        @Override
        public void setTerminal(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public void run(List<String> args) {
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.flush();
        }
    }

    public static class Bell implements Command, TerminalAware {

        private Terminal terminal;

        @Override
        public void setTerminal(Terminal terminal) {
            this.terminal = terminal;
        }
        @Override
        public void run(List<String> args) {
            terminal.puts(InfoCmp.Capability.bell);
            terminal.flush();
        }
    }
}
