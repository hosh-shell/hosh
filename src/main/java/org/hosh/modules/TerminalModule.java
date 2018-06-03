package org.hosh.modules;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import javax.annotation.Nonnull;
import java.util.List;

public class TerminalModule implements Module {

	@Override
	public void onStartup(@Nonnull CommandRegistry commandRegistry) {
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
