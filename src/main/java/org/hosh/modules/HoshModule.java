package org.hosh.modules;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.Record;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HoshModule implements Module {

	@Override
	public void onStartup(@Nonnull CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("env", Env.class);
		commandRegistry.registerCommand("info", Info.class);
		commandRegistry.registerCommand("exit", Exit.class);
	}

	// TODO: output here should be really key=value, actually only value is printed
	public static class Env implements Command {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			Map<String, String> env = System.getenv();
			for (Map.Entry<String, String> entry : env.entrySet()) {
				out.send(Record.empty().add(entry.getKey(), entry.getValue()));
			}
		}

	}

	public static class Info implements Command {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			out.send(Record.empty().add("timezone", TimeZone.getDefault().getID()));
			out.send(Record.empty().add("locale", Locale.getDefault()));
		}

	}

	public static class Exit implements Command {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			System.exit(0);
		}

	}

}
