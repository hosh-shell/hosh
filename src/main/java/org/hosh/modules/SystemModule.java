package org.hosh.modules;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.CommandWrapper;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Values;

public class SystemModule implements Module {
	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("echo", new Echo());
		commandRegistry.registerCommand("env", new Env());
	    commandRegistry.registerCommand("quit", new Exit());
		commandRegistry.registerCommand("exit", new Exit());
		commandRegistry.registerCommand("help", new Help());
		commandRegistry.registerCommand("sleep", new Sleep());
		commandRegistry.registerCommand("withTime", new WithTime());
	}

	public static class Echo implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel out, Channel err) {
			Record record = Record.of("text", Values.ofText(String.join("", args)));
			out.send(record);
			return ExitStatus.success();
		}
	}

	@Todo(description = "should print the variables stored in the state")
	public static class Env implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of("error", Values.ofText("expecting no parameters")));
				return ExitStatus.error();
			}
			Map<String, String> env = System.getenv();
			for (Map.Entry<String, String> entry : env.entrySet()) {
				Record record = Record.of("key", Values.ofText(entry.getKey()), "value",
						Values.ofText(entry.getValue()));
				out.send(record);
			}
			return ExitStatus.success();
		}
	}

	public static class Exit implements Command, StateAware {
        private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel out, Channel err) {
			switch (args.size()) {
				case 0:
					state.setExit(true);
					return ExitStatus.success();
				case 1:
					String arg = args.get(0);
					Optional<ExitStatus> exitStatus = ExitStatus.parse(arg);
					if (exitStatus.isPresent()) {
						state.setExit(true);
						return exitStatus.get();
					} else {
						err.send(Record.of("error", Values.ofText("not a valid exit status: " + arg)));
						return ExitStatus.error();
					}
				default:
					err.send(Record.of("error", Values.ofText("too many parameters")));
					return ExitStatus.error();
			}
		}
	}

	@Todo(description = "commands are not sorted by default, planning to use pipelines")
	public static class Help implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of("error", Values.ofText("expecting no parameters")));
				return ExitStatus.error();
			}
			Set<String> commands = state.getCommands().keySet();
			for (String command : commands) {
				out.send(Record.of("command", Values.ofText(command)));
			}
			return ExitStatus.success();
		}
	}

	@Todo(description = "use Duration as argument, we really need Value as arguments")
	public static class Sleep implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of("error", Values.ofText("expecting just one argument millis")));
				return ExitStatus.error();
			}
			String arg = args.get(0);
			try {
				Thread.sleep(Long.parseLong(arg));
				return ExitStatus.success();
			} catch (NumberFormatException e) {
				err.send(Record.of("error", Values.ofText("not millis: " + arg)));
				return ExitStatus.error();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				err.send(Record.of("error", Values.ofText("interrupted")));
				return ExitStatus.error();
			}
		}
	}

	public static class WithTime implements CommandWrapper<Long> {
		@Todo(description = "this is empty and looks like we have a design problem here")
		@Override
		public ExitStatus run(List<String> args, Channel out, Channel err) {
			return ExitStatus.success();
		}

		@Override
		public Long before(List<String> args, Channel out, Channel err) {
			return System.nanoTime();
		}

		@Override
		public void after(Long startNanos, Channel out, Channel err) {
			long endNanos = System.nanoTime();
			Duration duration = Duration.ofNanos(endNanos - startNanos);
			out.send(Record.of("message", Values.ofText("took " + duration)));
		}
	}
}
