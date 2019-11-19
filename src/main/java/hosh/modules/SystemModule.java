/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package hosh.modules;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.ProcessHandle.Info;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import hosh.spi.InputChannel;
import org.jline.reader.LineReader;

import hosh.doc.BuiltIn;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Experimental;
import hosh.spi.Ansi.Style;
import hosh.spi.OutputChannel;
import hosh.spi.Command;
import hosh.spi.CommandWrapper;
import hosh.spi.ExitStatus;
import hosh.spi.Key;
import hosh.spi.Keys;
import hosh.spi.LineReaderAware;
import hosh.spi.Module;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Values;

public class SystemModule implements Module {

	// keep in sync with Hosh.g4
	private static final Pattern VARIABLE = Pattern.compile("[A-Za-z_\\-]+");

	@BuiltIn(name = "echo", description = "write arguments to output")
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

	@BuiltIn(name = "env", description = "display all variables")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting no arguments")));
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

	@BuiltIn(name = "exit", description = "exit current interactive session or script")
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
						err.send(Records.singleton(Keys.ERROR, Values.ofText("not a valid exit status: " + arg)));
						return ExitStatus.error();
					}
				default:
					err.send(Records.singleton(Keys.ERROR, Values.ofText("too many arguments")));
					return ExitStatus.error();
			}
		}
	}

	@BuiltIn(name = "help", description = "built-in help system")
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
				for (var command : state.getCommands().entrySet()) {
					BuiltIn builtIn = command.getValue().getAnnotation(BuiltIn.class);
					String name = builtIn.name();
					String description = builtIn.description();
					Record record = Records.builder()
							.entry(Keys.NAME, Values.ofText(name))
							.entry(Keys.DESCRIPTION, Values.ofText(description))
							.build();
					out.send(record);
				}
				return ExitStatus.success();
			} else if (args.size() == 1) {
				String commandName = args.get(0);
				Class<? extends Command> commandClass = state.getCommands().get(commandName);
				if (commandClass == null) {
					err.send(Records.singleton(Keys.ERROR, Values.ofText("command not found: " + commandName)));
					return ExitStatus.error();
				}
				BuiltIn builtIn = commandClass.getAnnotation(BuiltIn.class);
				if (builtIn == null) {
					err.send(Records.singleton(Keys.ERROR, Values.ofText("no help for command: " + commandName)));
					return ExitStatus.error();
				}
				out.send(Records.singleton(Keys.TEXT, Values.ofStyledText(commandName + " - " + builtIn.description(), Style.BOLD)));
				Examples examples = commandClass.getAnnotation(Examples.class);
				out.send(Records.singleton(Keys.TEXT, Values.ofStyledText("Examples", Style.BOLD)));
				if (examples != null) {
					for (Example ex : examples.value()) {
						out.send(Records.singleton(Keys.TEXT, Values.ofStyledText(ex.command() + " # " + ex.description(), Style.ITALIC)));
					}
				} else {
					out.send(Records.singleton(Keys.TEXT, Values.ofStyledText("N/A", Style.FG_RED)));
				}
				return ExitStatus.success();
			} else {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("too many arguments")));
				return ExitStatus.error();
			}
		}
	}

	@BuiltIn(name = "sleep", description = "suspend execution for given duration, measured in millis")
	@Examples({
			@Example(command = "sleep 1000", description = "suspend execution for 1000 millis (1s)"),
	})
	public static class Sleep implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting just one argument millis")));
				return ExitStatus.error();
			}
			String arg = args.get(0);
			try {
				Thread.sleep(Long.parseLong(arg));
				return ExitStatus.success();
			} catch (NumberFormatException e) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("not millis: " + arg)));
				return ExitStatus.error();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				err.send(Records.singleton(Keys.ERROR, Values.ofText("interrupted")));
				return ExitStatus.error();
			}
		}
	}

	@BuiltIn(name = "withTime", description = "measure execution time of command or pipeline")
	@Examples({
			@Example(command = "withTime { ls }", description = "measure execution time of 'ls'"),
			@Example(command = "withTime { ls | sink }", description = "measure execution time of pipeline 'ls | sink'"),
	})
	public static class WithTime implements CommandWrapper<Long> {

		@Override
		public Long before(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			return System.nanoTime();
		}

		@Override
		public void after(Long startNanos, InputChannel in, OutputChannel out, OutputChannel err) {
			long endNanos = System.nanoTime();
			Duration duration = Duration.ofNanos(endNanos - startNanos);
			out.send(Records.singleton(Keys.DURATION, Values.ofDuration(duration)));
		}

		@Override
		public boolean retry(Long resource, InputChannel in, OutputChannel out, OutputChannel err) {
			return false;
		}
	}

	@BuiltIn(name = "ps", description = "process status")
	@Examples({
			@Example(command = "ps", description = "list all running process in the system as the current user"),
	})
	public static class ProcessList implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting zero arguments")));
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

	@BuiltIn(name = "kill", description = "kill process")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument")));
				return ExitStatus.error();
			}
			if (!args.get(0).matches("[0-9]+")) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("not a valid pid: " + args.get(0))));
				return ExitStatus.error();
			}
			long pid = Long.parseLong(args.get(0));
			Optional<ProcessHandle> process = processLookup.of(pid);
			if (process.isEmpty()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("cannot find pid: " + pid)));
				return ExitStatus.error();
			}
			boolean destroyed = process.get().destroy();
			if (!destroyed) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("cannot destroy pid: " + pid)));
				return ExitStatus.error();
			}
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "err", description = "create a runtime error, mostly for testing purposes")
	@Examples({
			@Example(command = "lines file.txt | err", description = "inject an error in this pipeline")
	})
	public static class Err implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			throw new NullPointerException("injected error: please do not report");
		}
	}

	@BuiltIn(name = "benchmark", description = "measure execution time (best, worst, average) of inner command")
	@Examples({
			@Example(command = "benchmark 50 { lines file.txt | sink } ", description = "repeat pipeline 50 times, measuring performance")
	})
	public static class Benchmark implements CommandWrapper<SystemModule.Benchmark.Accumulator> {

		public static final Key BEST = Keys.of("best");

		public static final Key WORST = Keys.of("worst");

		public static final Key AVERAGE = Keys.of("average");

		@Override
		public Accumulator before(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				throw new IllegalArgumentException("requires one integer arg");
			}
			int repeat = Integer.parseInt(args.get(0));
			Accumulator accumulator = new Accumulator(repeat);
			accumulator.start();
			return accumulator;
		}

		@Override
		public void after(Accumulator resource, InputChannel in, OutputChannel out, OutputChannel err) {
			Duration best = resource.results.stream().min(Comparator.naturalOrder()).orElse(Duration.ZERO);
			Duration worst = resource.results.stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);
			int runs = resource.results.size();
			Duration avg = runs == 0 ? Duration.ZERO : resource.results.stream().reduce(Duration.ZERO, (acc, d) -> acc.plus(d)).dividedBy(runs);
			out.send(Records.builder()
					.entry(Keys.COUNT, Values.ofNumeric(runs))
					.entry(BEST, Values.ofDuration(best))
					.entry(WORST, Values.ofDuration(worst))
					.entry(AVERAGE, Values.ofDuration(avg))
					.build());
		}

		@Override
		public boolean retry(Accumulator resource, InputChannel in, OutputChannel out, OutputChannel err) {
			resource.takeTime();
			return --resource.repeat > 0;
		}

		public static class Accumulator {

			private final List<Duration> results;

			private int repeat;

			private long nanoTime;

			public Accumulator(int repeat) {
				if (repeat <= 0) {
					throw new IllegalArgumentException("repeat must be > 0");
				}
				this.repeat = repeat;
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

			public List<Duration> getResults() {
				return results;
			}
		}
	}

	@BuiltIn(name = "sink", description = "consume any record (e.g. like /dev/null)")
	@Examples({
			@Example(command = "ls | sink", description = "consume any record produced by ls")
	})
	public static class Sink implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			while (true) {
				Optional<Record> recv = in.recv();
				if (recv.isEmpty()) {
					break;
				}
			}
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "set", description = "create or update a variable binding")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: set name value")));
				return ExitStatus.error();
			}
			String key = args.get(0);
			String value = args.get(1);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
				return ExitStatus.error();
			}
			state.getVariables().put(key, value);
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "unset", description = "delete a variable binding")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("requires 1 argument: key")));
				return ExitStatus.error();
			}
			String key = args.get(0);
			state.getVariables().remove(key);
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "input", description = "Read a string from standard input and assign result to variable. The trailing newline is stripped.")
	@Examples({
			@Example(command = "input FOO", description = "save string read to variable 'FOO'"),
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: input VARIABLE")));
				return ExitStatus.error();
			}
			String key = args.get(0);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
				return ExitStatus.error();
			}
			Optional<String> read = input();
			if (read.isEmpty()) {
				return ExitStatus.error();
			}
			state.getVariables().put(key, read.get());
			return ExitStatus.success();
		}

		private Optional<String> input() {
			try {
				return Optional.of(lineReader.readLine("input> "));
			} catch (Exception e) {
				return Optional.empty();
			}
		}
	}

	@Experimental(description = "probably should be merged with input? not sure about name")
	@BuiltIn(name = "secret", description = "Read a string from standard input in a secure way and assign result to variable. The trailing newline is stripped.")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: secret VARIABLE")));
				return ExitStatus.error();
			}
			String key = args.get(0);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
				return ExitStatus.error();
			}
			Optional<String> read = secret();
			if (read.isEmpty()) {
				return ExitStatus.error();
			}
			state.getVariables().put(key, read.get());
			return ExitStatus.success();
		}

		private Optional<String> secret() {
			try {
				return Optional.of(lineReader.readLine('\0'));
			} catch (Exception e) {
				return Optional.empty();
			}
		}
	}

	@BuiltIn(name = "capture", description = "capture output of a command into a variable")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: capture VARNAME")));
				return ExitStatus.error();
			}
			Locale locale = Locale.getDefault();
			String key = args.get(0);
			if (!VARIABLE.matcher(key).matches()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
				return ExitStatus.error();
			}
			StringWriter result = new StringWriter();
			PrintWriter pw = new PrintWriter(result);
			for (;;) {
				Optional<Record> recv = in.recv();
				if (recv.isEmpty()) {
					break;
				}
				Record record = recv.get();
				record.print(pw, locale);
			}
			state.getVariables().put(key, result.toString());
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "open", description = "send output of a command into a file")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: open filename [WRITE|APPEND|...]")));
				return ExitStatus.error();
			}
			Locale locale = Locale.getDefault();
			Path path = state.getCwd().resolve(Paths.get(args.get(0)));
			try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(path, toOpenOptions(args)), StandardCharsets.UTF_8))) {
				for (;;) {
					Optional<Record> recv = in.recv();
					if (recv.isEmpty()) {
						break;
					}
					Record record = recv.get();
					record.print(pw, locale);
					pw.append(System.lineSeparator());
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
					.map(arg -> parseOption(arg))
					.toArray(OpenOption[]::new);
		}

		private OpenOption parseOption(String arg) {
			return Enum.valueOf(StandardOpenOption.class, arg);
		}
	}
}
