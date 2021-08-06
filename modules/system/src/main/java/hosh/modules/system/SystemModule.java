/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.modules.system;

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Experimental;
import hosh.spi.Ansi.Style;
import hosh.spi.Command;
import hosh.spi.CommandRegistry;
import hosh.spi.CommandWrapper;
import hosh.spi.Errors;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Key;
import hosh.spi.Keys;
import hosh.spi.LineReaderAware;
import hosh.spi.Module;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Values;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.ProcessHandle.Info;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class SystemModule implements Module {

	// please keep in sync with HoshParser.g4 and HoshLexer.g4
	private static final Pattern VARIABLE = Pattern.compile("[A-Za-z_\\-]+");

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand("path", Path::new);
		registry.registerCommand("echo", Echo::new);
		registry.registerCommand("env", Env::new);
		registry.registerCommand("exit", Exit::new);
		registry.registerCommand("help", Help::new);
		registry.registerCommand("sleep", Sleep::new);
		registry.registerCommand("withTime", WithTime::new);
		registry.registerCommand("withTimeout", WithTimeout::new);
		registry.registerCommand("ps", ProcessList::new);
		registry.registerCommand("kill", KillProcess::new);
		registry.registerCommand("err", Err::new);
		registry.registerCommand("benchmark", Benchmark::new);
		registry.registerCommand("waitSuccess", WaitSuccess::new);
		registry.registerCommand("sink", Sink::new);
		registry.registerCommand("set", SetVariable::new);
		registry.registerCommand("unset", UnsetVariable::new);
		registry.registerCommand("input", Input::new);
		registry.registerCommand("secret", Secret::new);
		registry.registerCommand("confirm", Confirm::new);
		registry.registerCommand("capture", Capture::new);
		registry.registerCommand("open", Open::new);
	}

	@Description("PATH manipulation")
	@Examples({
		@Example(command = "path show", description = "show path"),
	})
	public static class Path implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.isEmpty()) {
				err.send(Errors.usage("path [show|clear|append path|prepend path]"));
				return ExitStatus.error();
			}
			String command = args.get(0);
			switch (command) {
				case "show": {
					if (args.size() != 1) {
						err.send(Errors.usage("path show"));
						return ExitStatus.error();
					}
					for (var path : state.getPath()) {
						out.send(Records.singleton(Keys.PATH, Values.ofPath(path)));
					}
					return ExitStatus.success();
				}

				case "clear": {
					if (args.size() != 1) {
						err.send(Errors.usage("path clear"));
						return ExitStatus.error();
					}

					state.getPath().clear();
					return ExitStatus.success();
				}

				case "append": {
					if (args.size() != 2) {
						err.send(Errors.usage("path append path"));
						return ExitStatus.error();
					}
					var path = Paths.get(args.get(1));
					state.getPath().add(path);
					return ExitStatus.success();
				}

				case "prepend": {
					if (args.size() != 2) {
						err.send(Errors.usage("path prepend path"));
						return ExitStatus.error();
					}
					var path = Paths.get(args.get(1));
					state.getPath().add(0, path);
					return ExitStatus.success();
				}

				default: {
					err.send(Errors.usage("path [show|clear|append path|prepend path]"));
					return ExitStatus.error();
				}
			}
		}
	}

	@Description("write arguments to output")
	@Examples({
		@Example(command = "echo", description = "write empty line"),
		@Example(command = "echo hello", description = "write 'hello'"),
		@Example(command = "echo hello ${USER}", description = "write 'hello dfa', if USER=dfa"),
	})
	public static class Echo implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			Record record = Records.singleton(Keys.VALUE, Values.ofText(String.join(" ", args)));
			out.send(record);
			return ExitStatus.success();
		}
	}

	@Description("display all variables")
	@Examples({
		@Example(command = "env", description = "display all environment variables"),
	})
	public static class Env implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (!args.isEmpty()) {
				err.send(Errors.usage("env"));
				return ExitStatus.error();
			}
			Map<String, String> variables = state.getVariables();
			for (var entry : variables.entrySet()) {
				Record record = Records.builder()
					.entry(Keys.NAME, Values.ofText(entry.getKey()))
					.entry(Keys.VALUE, Values.ofText(entry.getValue()))
					.build();
				out.send(record);
			}
			return ExitStatus.success();
		}
	}

	@Description("exit current interactive session or script")
	@Examples({
		@Example(command = "exit", description = "exit with status 0 (success)"),
		@Example(command = "exit 1", description = "exit with status 1 (error)"),
		@Example(command = "exit 0", description = "exit with status 0 (success)"),
	})
	public static class Exit implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
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
						err.send(Errors.message("not a valid exit status: %s", arg));
						return ExitStatus.error();
					}
				default:
					err.send(Errors.usage("exit [value]"));
					return ExitStatus.error();
			}
		}
	}

	@Description("built-in help system")
	@Examples({
		@Example(command = "help", description = "print all built-in commands"),
		@Example(command = "help command", description = "print help for specified command")
	})
	public static class Help implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.isEmpty()) {
				for (var entry : state.getCommands().entrySet()) {
					Description description = entry.getValue().get().getClass().getAnnotation(Description.class);
					String name = entry.getKey();
					Record record = Records.builder()
						.entry(Keys.NAME, Values.ofText(name))
						.entry(Keys.DESCRIPTION, Values.ofText(description.value()))
						.build();
					out.send(record);
				}
				return ExitStatus.success();
			} else if (args.size() == 1) {
				String commandName = args.get(0);
				Supplier<Command> commandSupplier = state.getCommands().get(commandName);
				if (commandSupplier == null) {
					err.send(Errors.message("command not found: %s", commandName));
					return ExitStatus.error();
				}
				Class<? extends Command> commandClass = commandSupplier.get().getClass();
				Description builtIn = commandClass.getAnnotation(Description.class);
				if (builtIn == null) {
					err.send(Errors.message("no help for command: %s", commandName));
					return ExitStatus.error();
				}
				out.send(Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText(commandName + " - " + builtIn.value()), Style.BOLD)));
				Examples examples = commandClass.getAnnotation(Examples.class);
				out.send(Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("Examples"), Style.BOLD)));
				if (examples != null) {
					for (Example ex : examples.value()) {
						out.send(Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText(ex.command() + " # " + ex.description()), Style.ITALIC)));
					}
				} else {
					out.send(Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("N/A"), Style.FG_RED)));
				}
				return ExitStatus.success();
			} else {
				err.send(Errors.usage("help [command]"));
				return ExitStatus.error();
			}
		}
	}

	@Description("suspend execution for given duration")
	@Examples({
		@Example(command = "sleep 2s", description = "suspend execution for 2 seconds (using ISO 8061 without PT prefix)"),
		@Example(command = "sleep PT1M", description = "suspend execution for 1 minute (using ISO 8601)"),
	})
	public static class Sleep implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("sleep duration"));
				return ExitStatus.error();
			}
			Optional<Duration> duration = DurationParsing.parse(args.get(0));
			if (duration.isEmpty()) {
				err.send(Errors.message("invalid duration: '%s'", args.get(0)));
				return ExitStatus.error();
			}
			try {
				Thread.sleep(duration.get().toMillis());
				return ExitStatus.success();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				err.send(Errors.message("interrupted"));
				return ExitStatus.error();
			}
		}
	}

	@Description("measure execution time of command or pipeline")
	@Examples({
		@Example(command = "withTime { ls }", description = "measure execution time of 'ls'"),
		@Example(command = "withTime { ls | sink }", description = "measure execution time of pipeline 'ls | sink'"),
	})
	public static class WithTime implements CommandWrapper {

		private NestedCommand nestedCommand;

		@Override
		public void setNestedCommand(NestedCommand nestedCommand) {
			this.nestedCommand = nestedCommand;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Errors.usage("withTime { ... }"));
				return ExitStatus.error();
			}
			long startNanos = System.nanoTime();
			ExitStatus exitStatus = nestedCommand.run();
			long endNanos = System.nanoTime();
			Duration duration = Duration.ofNanos(endNanos - startNanos);
			out.send(Records.singleton(Keys.DURATION, Values.ofDuration(duration)));
			return exitStatus;
		}
	}

	@Description("process status")
	@Examples({
		@Example(command = "ps", description = "list all running process in the system as the current user"),
	})
	public static class ProcessList implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Errors.usage("ps"));
				return ExitStatus.error();
			}
			ProcessHandle.allProcesses().forEach(process -> {
				Info info = process.info();
				Record result = Records.builder()
					.entry(Keys.of("pid"), Values.ofNumeric(process.pid()))
					.entry(Keys.of("user"), Values.ofText(info.user().orElse("-")))
					.entry(Keys.TIMESTAMP, info.startInstant().map(Values::ofInstant).orElse(Values.none()))
					.entry(Keys.of("command"), Values.ofText(info.command().orElse("-")))
					.entry(Keys.of("arguments"), Values.ofText(String.join(" ", info.arguments().orElse(new String[0]))))
					.build();
				out.send(result);
			});
			return ExitStatus.success();
		}
	}

	@Description("kill process")
	@Examples({
		@Example(command = "kill 38878", description = "kill process with PID 38878"),
	})
	public static class KillProcess implements Command {

		/**
		 * Allows unit testing by breaking hard dependency to ProcessHandle.
		 */
		public interface ProcessLookup {

			Optional<ProcessHandle> of(long pid);
		}

		private ProcessLookup processLookup = ProcessHandle::of;

		public void setProcessLookup(ProcessLookup processLookup) {
			this.processLookup = processLookup;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("kill process"));
				return ExitStatus.error();
			}
			if (!args.get(0).matches("[0-9]+")) {
				err.send(Errors.message("not a valid pid: %s", args.get(0)));
				return ExitStatus.error();
			}
			long pid = Long.parseLong(args.get(0));
			Optional<ProcessHandle> process = processLookup.of(pid);
			if (process.isEmpty()) {
				err.send(Errors.message("cannot find pid: %s", pid));
				return ExitStatus.error();
			}
			boolean destroyed = process.get().destroy();
			if (!destroyed) {
				err.send(Errors.message("cannot destroy pid: %s", pid));
				return ExitStatus.error();
			}
			return ExitStatus.success();
		}
	}

	@Description("create a runtime error (exception), mostly for testing purposes")
	@Examples({
		@Example(command = "lines file.txt | err", description = "the pipeline will fail")
	})
	public static class Err implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			throw new NullPointerException("please do not report: this is a simulated error");
		}
	}

	@Experimental(description = "support same syntax for duration as sleep")
	@Description("run command with a timeout")
	@Examples({
		@Example(command = "withTimeout 5s { walk / } ", description = "try to walk the entire filesystem within 5s timeout"),
		@Example(command = "withTimeout PT5s { walk / } ", description = "try to walk the entire filesystem within 5s timeout")
	})
	public static class WithTimeout implements CommandWrapper {

		private NestedCommand nestedCommand;

		@Override
		public void setNestedCommand(NestedCommand nestedCommand) {
			this.nestedCommand = nestedCommand;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("withTimeout duration { ... }"));
				return ExitStatus.error();
			}
			Optional<Duration> timeout = DurationParsing.parse(args.get(0));
			if (timeout.isEmpty()) {
				err.send(Errors.message("invalid duration: '%s'", args.get(0)));
				return ExitStatus.error();
			}
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			Future<ExitStatus> future = executorService.submit(nestedCommand::run);
			try {
				return future.get(timeout.get().toMillis(), TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				err.send(Errors.message("interrupted"));
				return ExitStatus.error();
			} catch (TimeoutException e) {
				err.send(Errors.message("timeout"));
				return ExitStatus.error();
			} catch (ExecutionException e) {
				err.send(Errors.message(e));
				return ExitStatus.error();
			} finally {
				executorService.shutdownNow();
			}
		}

	}

	@Description("repeat command until the first success")
	@Examples({
		@Example(command = "waitSuccess { http http://localhost:8080/ } ", description = "waiting for local service on port 8080, waiting 1s between attempts (default)"),
		@Example(command = "waitSuccess 5s { http http://localhost:8080/ } ", description = "waiting for local service on port 8080, waiting 5s between attempts"),
		@Example(command = "waitSuccess PS5s { http http://localhost:8080/ } ", description = "waiting for local service on port 8080, waiting 5s between attempts")
	})
	public static class WaitSuccess implements CommandWrapper {

		private NestedCommand nestedCommand;

		@Override
		public void setNestedCommand(NestedCommand nestedCommand) {
			this.nestedCommand = nestedCommand;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() >= 2) {
				err.send(Errors.usage("waitSuccess [duration] { ... }"));
				return ExitStatus.error();
			}
			Duration sleep;
			if (args.size() == 0) {
				sleep = Duration.ofSeconds(1);
			} else {
				Optional<Duration> maybeSleep = DurationParsing.parse(args.get(0));
				if (maybeSleep.isEmpty()) {
					err.send(Errors.message("invalid duration: '%s'", args.get(0)));
					return ExitStatus.error();
				} else {
					sleep = maybeSleep.get();
				}
			}
			while (true) {
				ExitStatus exitStatus = nestedCommand.run();
				if (exitStatus.isSuccess()) {
					return exitStatus;
				} else {
					try {
						//noinspection BusyWait
						Thread.sleep(sleep.toMillis());
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						err.send(Errors.message("interrupted"));
						return ExitStatus.error();
					}
				}
			}
		}
	}

	@Description("measure execution time (best, worst, average) of inner command")
	@Examples({
		@Example(command = "benchmark 50 { lines file.txt | sink } ", description = "repeat pipeline 50 times, measuring performance")
	})
	public static class Benchmark implements CommandWrapper {

		public static final Key BEST = Keys.of("best");

		public static final Key WORST = Keys.of("worst");

		public static final Key AVERAGE = Keys.of("average");

		private NestedCommand nestedCommand;

		@Override
		public void setNestedCommand(NestedCommand nestedCommand) {
			this.nestedCommand = nestedCommand;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("benchmark number { ... }"));
				return ExitStatus.error();
			}

			int repeat = Integer.parseInt(args.get(0));

			if (repeat <= 0) {
				err.send(Errors.message("number must be >= 0"));
				return ExitStatus.error();
			}
			Accumulator accumulator = new Accumulator(repeat);
			accumulator.start();

			for (int i = 0; i < repeat; i++) {
				// ignoring exit status
				nestedCommand.run();
				accumulator.takeTime();
			}

			Duration best = accumulator.results.stream().min(Comparator.naturalOrder()).orElse(Duration.ZERO);
			Duration worst = accumulator.results.stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);
			int runs = accumulator.results.size();
			Duration avg = runs == 0 ? Duration.ZERO : accumulator.results.stream().reduce(Duration.ZERO, Duration::plus).dividedBy(runs);
			out.send(Records.builder()
				.entry(Keys.COUNT, Values.ofNumeric(runs))
				.entry(BEST, Values.ofDuration(best))
				.entry(WORST, Values.ofDuration(worst))
				.entry(AVERAGE, Values.ofDuration(avg))
				.build());
			return ExitStatus.success();
		}

		public static class Accumulator {

			private final List<Duration> results;

			private long nanoTime;

			public Accumulator(int repeat) {
				this.results = new ArrayList<>(repeat);
			}

			public void start() {
				nanoTime = System.nanoTime();
			}

			public void takeTime() {
				Duration elapsed = Duration.ofNanos(System.nanoTime() - nanoTime);
				results.add(elapsed);
				start();
			}

		}
	}

	@Description("consume any record (e.g. like /dev/null)")
	@Examples({
		@Example(command = "ls | sink", description = "consume any record produced by ls")
	})
	public static class Sink implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			for (Record ignored : InputChannel.iterate(in)) {
				blackHole(ignored);
			}
			return ExitStatus.success();
		}

		private void blackHole(Record record) { // NOSONAR: by design
			record.size();
		}

	}

	@Description("create or update a variable binding")
	@Examples({
		@Example(command = "set FILE file.txt", description = "create variable FILE"),
		@Example(command = "set FILE another_file.txt", description = "update variable FILE"),
	})
	public static class SetVariable implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Errors.usage("set variable value"));
				return ExitStatus.error();
			}
			String key = args.get(0);
			String value = args.get(1);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Errors.message("invalid variable name"));
				return ExitStatus.error();
			}
			state.getVariables().put(key, value);
			return ExitStatus.success();
		}
	}

	@Description("delete a variable binding")
	@Examples({
		@Example(command = "unset FILE", description = "delete variable FILE, cannot be referenced anymore after this command"),
	})
	public static class UnsetVariable implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("unset variable"));
				return ExitStatus.error();
			}
			String key = args.get(0);
			state.getVariables().remove(key);
			return ExitStatus.success();
		}
	}

	@Description("Read a string from standard input and assign result to variable. The trailing newline is stripped.")
	@Examples({
		@Example(command = "input FILE", description = "save string read to variable 'FILE'"),
	})
	public static class Input implements Command, StateAware, LineReaderAware {

		private State state;

		private LineReader lineReader;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void setLineReader(LineReader lineReader) {
			this.lineReader = lineReader;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("input variable"));
				return ExitStatus.error();
			}
			String key = args.get(0);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Errors.message("invalid variable name"));
				return ExitStatus.error();
			}
			Optional<String> maybeInput = readInput();
			if (maybeInput.isEmpty()) {
				return ExitStatus.error();
			}
			state.getVariables().put(key, maybeInput.get());
			return ExitStatus.success();
		}

		private Optional<String> readInput() {
			try {
				return Optional.of(lineReader.readLine("input> "));
			} catch (UserInterruptException | EndOfFileException e) {
				return Optional.empty();
			}
		}
	}

	@Description("Read a string from standard input in a secure way and assign result to variable. The trailing newline is stripped.")
	@Examples({
		@Example(command = "secret PASSWORD", description = "save string read to variable 'PASSWORD'"),
	})
	public static class Secret implements Command, StateAware, LineReaderAware {

		private State state;

		private LineReader lineReader;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void setLineReader(LineReader lineReader) {
			this.lineReader = lineReader;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("secret variable"));
				return ExitStatus.error();
			}
			String key = args.get(0);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Errors.message("invalid variable name"));
				return ExitStatus.error();
			}
			Optional<String> read = readSecret();
			if (read.isEmpty()) {
				return ExitStatus.error();
			}
			state.getVariables().put(key, read.get());
			return ExitStatus.success();
		}

		private Optional<String> readSecret() {
			try {
				// '\0' means "noecho"
				return Optional.of(lineReader.readLine('\0'));
			} catch (UserInterruptException | EndOfFileException e) {
				return Optional.empty();
			}
		}
	}

	@Description("Ask a question and wait for a user confirmation (Y/N)")
	@Examples({
		@Example(command = "confirm 'continue?'", description = "display question then wait for user input"),
	})
	public static class Confirm implements Command, LineReaderAware {

		private LineReader lineReader;

		@Override
		public void setLineReader(LineReader lineReader) {
			this.lineReader = lineReader;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("confirm message"));
				return ExitStatus.error();
			}
			String message = args.get(0);
			Optional<String> maybeAnswer = readAnswer(message + " (Y/N)? ");
			if (maybeAnswer.isEmpty()) {
				return ExitStatus.error();
			}
			String answer = maybeAnswer.get();
			if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
				return ExitStatus.success();
			} else if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
				return ExitStatus.error();
			} else {
				err.send(Errors.message("invalid answer"));
				return ExitStatus.error();
			}
		}

		private Optional<String> readAnswer(String prompt) {
			try {
				return Optional.of(lineReader.readLine(prompt));
			} catch (UserInterruptException | EndOfFileException e) {
				return Optional.empty();
			}
		}
	}

	@Description("capture output of a command into a variable")
	@Examples({
		@Example(command = "cwd | capture CWD", description = "create or update CWD variable with the output of 'cwd' command"),
	})
	@Experimental(description = "too low level compared to simply VARNAME=$(ls)")
	public static class Capture implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("capture variable"));
				return ExitStatus.error();
			}
			String key = args.get(0);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Errors.message("invalid variable name"));
				return ExitStatus.error();
			}
			Locale locale = Locale.getDefault();
			StringWriter result = new StringWriter();
			PrintWriter pw = new PrintWriter(result);
			for (Record incoming : InputChannel.iterate(in)) {
				incoming.print(pw, locale);
			}
			state.getVariables().put(key, result.toString());
			return ExitStatus.success();
		}
	}

	@Description("send output of a command into a file")
	@Examples({
		@Example(command = "cwd | open cwd.txt CREATE WRITE ", description = "write output of 'cwd' command to a file named 'whoami.txt'")
	})
	@Experimental(description = "too low level compared to simply > file.txt or >> file.txt? too much power for end user (e.g. they could use DSYNC or READ)?")
	public static class Open implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() <= 2) {
				err.send(Errors.usage("open file [WRITE|APPEND|...]"));
				return ExitStatus.error();
			}
			Locale locale = Locale.getDefault();
			var path = state.getCwd().resolve(Paths.get(args.get(0)));
			try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(path, toOpenOptions(args)), StandardCharsets.UTF_8))) {
				for (Record incoming : InputChannel.iterate(in)) {
					incoming.print(pw, locale);
					pw.println();
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private OpenOption[] toOpenOptions(List<String> args) {
			return args
				.stream()
				.skip(1)
				.map(this::parseOption)
				.toArray(OpenOption[]::new);
		}

		private OpenOption parseOption(String arg) {
			return Enum.valueOf(StandardOpenOption.class, arg);
		}
	}
}
