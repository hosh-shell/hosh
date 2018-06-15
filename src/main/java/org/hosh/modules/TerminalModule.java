package org.hosh.modules;

import java.util.List;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

public class TerminalModule implements Module {

	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("clear", new Clear());
		commandRegistry.registerCommand("bell", new Bell());
	}

	public static class Clear implements Command, TerminalAware {

		private Terminal terminal;

		@Override
		public void setTerminal(Terminal terminal) {
			this.terminal = terminal;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
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
		public void run(List<String> args, Channel out, Channel err) {
			terminal.puts(InfoCmp.Capability.bell);
			terminal.flush();
		}
	}
}
