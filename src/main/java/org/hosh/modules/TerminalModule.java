package org.hosh.modules;

import java.util.List;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.TerminalAware;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

public class TerminalModule implements Module {

	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("dump", new Dump());
		commandRegistry.registerCommand("clear", new Clear());
		commandRegistry.registerCommand("bell", new Bell());
	}

	public static class Dump implements Command, TerminalAware {

		private Terminal terminal;

		@Override
		public void setTerminal(Terminal terminal) {
			this.terminal = terminal;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			out.send(Record.of("name", Values.ofText(terminal.getName())));
			out.send(Record.of("type", Values.ofText(terminal.getType())));
			out.send(Record.of("attributes", Values.ofText(terminal.getAttributes().toString())));
		}
	}

	public static class Clear implements Command, TerminalAware {

		private Terminal terminal;

		@Override
		public void setTerminal(Terminal terminal) {
			this.terminal = terminal;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of("error", Values.ofText("no parameters expected")));
				return;
			}
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
			if (!args.isEmpty()) {
				err.send(Record.of("error", Values.ofText("no parameters expected")));
				return;
			}
			terminal.puts(InfoCmp.Capability.bell);
			terminal.flush();
		}
	}
}
