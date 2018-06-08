package org.hosh.modules;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class HoshModule implements Module {

	@Override
	public void onStartup(@Nonnull CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("env", Env.class);
		commandRegistry.registerCommand("info", Info.class);
		commandRegistry.registerCommand("exit", Exit.class);
		commandRegistry.registerCommand("help", Help.class);

	}

	// TODO: output here should be really key=value, actually only value is printed
	public static class Env implements Command {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			Map<String, String> env = System.getenv();
			for (Map.Entry<String, String> entry : env.entrySet()) {
				out.send(Record.of(entry.getKey(), entry.getValue()));
			}
		}

	}

	public static class Info implements Command {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			out.send(Record.of("timezone", TimeZone.getDefault().getID()));
			out.send(Record.of("locale", Locale.getDefault()));
		}

	}

	public static class Exit implements Command {

		// TODO: looks like we need to an argument parsing library
		@Override
		public void run(List<String> args, Channel out, Channel err) {
			if (args.size() == 0) {
				System.exit(0);
			} else if (args.size() == 1) {
				String arg = args.get(0);
				if (arg.matches("\\d{1,3}")) {
					System.exit(Integer.parseInt(arg));
				} else {
					err.send(Record.of("error", "arg must be a number (0-999)"));
				}
			} else {
				err.send(Record.of("error", "too many parameters"));
			}
		}

	}

	// TODO: commands are not sorted by default (let's wait for "| sortBy key" syntax)
	public static class Help implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			Set<String> commands = state.getCommands().keySet();
			for (String command : commands) {
				out.send(Record.of("command", command));
			}
		}

	}
}
