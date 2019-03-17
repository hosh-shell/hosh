/**
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
package org.hosh.modules;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hosh.doc.Experimental;
import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.CommandWrapper;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Key;
import org.hosh.spi.Keys;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Value;
import org.hosh.spi.Values;

public class SystemModule implements Module {
	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("echo", Echo.class);
		commandRegistry.registerCommand("env", Env.class);
		commandRegistry.registerCommand("quit", Exit.class);
		commandRegistry.registerCommand("exit", Exit.class);
		commandRegistry.registerCommand("help", Help.class);
		commandRegistry.registerCommand("sleep", Sleep.class);
		commandRegistry.registerCommand("withTime", WithTime.class);
		commandRegistry.registerCommand("ps", ProcessList.class);
		commandRegistry.registerCommand("kill", KillProcess.class);
		commandRegistry.registerCommand("err", Err.class);
		commandRegistry.registerCommand("benchmark", Benchmark.class);
		commandRegistry.registerCommand("source", Source.class);
		commandRegistry.registerCommand("sink", Sink.class);
		commandRegistry.registerCommand("set", SetVariable.class);
		commandRegistry.registerCommand("unset", UnsetVariable.class);
		commandRegistry.registerCommand("capture", CaptureVariable.class);
		commandRegistry.registerCommand("open", Open.class);
	}

	public static class Echo implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			Record record = Record.of(Keys.VALUE, Values.ofText(String.join(" ", args)));
			out.send(record);
			return ExitStatus.success();
		}
	}

	public static class Env implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting no parameters")));
				return ExitStatus.error();
			}
			Map<String, String> env = state.getVariables();
			for (Map.Entry<String, String> entry : env.entrySet()) {
				Record record = Record.builder()
						.entry(Keys.NAME, Values.ofText(entry.getKey()))
						.entry(Keys.VALUE, Values.ofText(entry.getValue()))
						.build();
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
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
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
						err.send(Record.of(Keys.ERROR, Values.ofText("not a valid exit status: " + arg)));
						return ExitStatus.error();
					}
				default:
					err.send(Record.of(Keys.ERROR, Values.ofText("too many parameters")));
					return ExitStatus.error();
			}
		}
	}

	public static class Help implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting no parameters")));
				return ExitStatus.error();
			}
			Set<String> commands = state.getCommands().keySet();
			for (String command : commands) {
				out.send(Record.of(Keys.of("command"), Values.ofText(command)));
			}
			return ExitStatus.success();
		}
	}

	public static class Sleep implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting just one argument millis")));
				return ExitStatus.error();
			}
			String arg = args.get(0);
			try {
				Thread.sleep(Long.parseLong(arg));
				return ExitStatus.success();
			} catch (NumberFormatException e) {
				err.send(Record.of(Keys.ERROR, Values.ofText("not millis: " + arg)));
				return ExitStatus.error();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				err.send(Record.of(Keys.ERROR, Values.ofText("interrupted")));
				return ExitStatus.error();
			}
		}
	}

	public static class WithTime implements CommandWrapper<Long> {
		@Override
		public Long before(List<String> args, Channel in, Channel out, Channel err) {
			return System.nanoTime();
		}

		@Override
		public void after(Long startNanos, Channel in, Channel out, Channel err) {
			long endNanos = System.nanoTime();
			Duration duration = Duration.ofNanos(endNanos - startNanos);
			out.send(Record.of(Keys.DURATION, Values.ofDuration(duration)));
		}
	}

	public static class ProcessList implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting zero arguments")));
				return ExitStatus.error();
			}
			ProcessHandle.allProcesses().forEach(process -> {
				Info info = process.info();
				Record result = Record.builder()
						.entry(Keys.of("pid"), Values.ofNumeric(process.pid()))
						.entry(Keys.of("user"), Values.ofText(info.user().orElse("-")))
						.entry(Keys.of("start"), info.startInstant().map(Values::ofInstant).orElse(Values.none()))
						.entry(Keys.of("command"), Values.ofText(info.command().orElse("-")))
						.entry(Keys.of("arguments"), Values.ofText(String.join(" ", info.arguments().orElse(new String[0]))))
						.build();
				out.send(result);
			});
			return ExitStatus.success();
		}
	}

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
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting one argument")));
				return ExitStatus.error();
			}
			if (!args.get(0).matches("[0-9]+")) {
				err.send(Record.of(Keys.ERROR, Values.ofText("not a valid pid: " + args.get(0))));
				return ExitStatus.error();
			}
			long pid = Long.parseLong(args.get(0));
			Optional<ProcessHandle> process = processLookup.of(pid);
			if (process.isEmpty()) {
				err.send(Record.of(Keys.ERROR, Values.ofText("cannot find pid: " + pid)));
				return ExitStatus.error();
			}
			boolean destroyed = process.get().destroy();
			if (!destroyed) {
				err.send(Record.of(Keys.ERROR, Values.ofText("cannot destroy pid: " + pid)));
				return ExitStatus.error();
			}
			return ExitStatus.success();
		}
	}

	public static class Err implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			throw new NullPointerException("injected error: please do not report");
		}
	}

	public static class Benchmark implements CommandWrapper<SystemModule.Benchmark.Accumulator> {
		public static final Key BEST = Keys.of("best");
		public static final Key WORST = Keys.of("worst");
		public static final Key AVERAGE = Keys.of("average");

		@Override
		public Accumulator before(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				throw new IllegalArgumentException("requires one integer arg");
			}
			int repeat = Integer.parseInt(args.get(0));
			Accumulator accumulator = new Accumulator(repeat);
			accumulator.start();
			return accumulator;
		}

		@Override
		public void after(Accumulator resource, Channel in, Channel out, Channel err) {
			Duration best = resource.results.stream().min(Comparator.naturalOrder()).orElse(Duration.ZERO);
			Duration worst = resource.results.stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);
			int runs = resource.results.size();
			Duration avg = runs == 0 ? Duration.ZERO : resource.results.stream().reduce(Duration.ZERO, (acc, d) -> acc.plus(d)).dividedBy(runs);
			out.send(Record.builder()
					.entry(Keys.COUNT, Values.ofNumeric(runs))
					.entry(BEST, Values.ofDuration(best))
					.entry(WORST, Values.ofDuration(worst))
					.entry(AVERAGE, Values.ofDuration(avg))
					.build());
		}

		@Override
		public boolean retry(Accumulator resource, Channel in, Channel out, Channel err) {
			// allows to check for interruptions (i.e. SIGINT)
			Thread.yield();
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

	public static class Source implements Command {
		@SuppressWarnings("squid:S2189")
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			while (true) {
				out.send(Record.of(Keys.VALUE, Values.ofText("test value")));
			}
		}
	}

	public static class Sink implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			while (true) {
				Optional<Record> recv = in.recv();
				if (recv.isEmpty()) {
					break;
				}
			}
			return ExitStatus.success();
		}
	}

	public static class SetVariable implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Record.of(Keys.ERROR, Values.ofText("requires 2 arguments: key value")));
				return ExitStatus.error();
			}
			String key = args.get(0);
			String value = args.get(1);
			state.getVariables().put(key, value);
			return ExitStatus.success();
		}
	}

	public static class UnsetVariable implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("requires 1 argument: key")));
				return ExitStatus.error();
			}
			String key = args.get(0);
			state.getVariables().remove(key);
			return ExitStatus.success();
		}
	}

	@Todo(description = "this could be used by compiler as implementation for syntax sugar")
	@Experimental(description = "too low level compared to simply VARNAME=$(ls)")
	public static class CaptureVariable implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("usage: capture VARNAME")));
				return ExitStatus.error();
			}
			Locale locale = Locale.getDefault();
			String key = args.get(0);
			StringWriter result = new StringWriter();
			PrintWriter printWriter = new PrintWriter(result);
			for (;;) {
				Optional<Record> recv = in.recv();
				if (recv.isEmpty()) {
					break;
				}
				Record record = recv.get();
				// see RecordWriter
				Iterator<Value> iterator = record.values().iterator();
				while (iterator.hasNext()) {
					Value value = iterator.next();
					value.append(printWriter, locale);
					if (iterator.hasNext()) {
						printWriter.append(' ');
					}
				}
			}
			state.getVariables().put(key, result.toString());
			return ExitStatus.success();
		}
	}

	@Todo(description = "this could be used by compiler as implementation for syntax sugar")
	@Experimental(description = "too low level compared to simply > file.txt or >> file.txt? too much power for end user (e.g. they could use DSYNC or READ)?")
	public static class Open implements Command, StateAware {
		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() <= 2) {
				err.send(Record.of(Keys.ERROR, Values.ofText("usage: filename [WRITE|APPEND|...]")));
				return ExitStatus.error();
			}
			Locale locale = Locale.getDefault();
			Path path = state.getCwd().resolve(Paths.get(args.get(0)));
			try (PrintWriter writer = new PrintWriter(
					new OutputStreamWriter(Files.newOutputStream(path, toOpenOptions(args)), StandardCharsets.UTF_8))) {
				for (;;) {
					Optional<Record> recv = in.recv();
					if (recv.isEmpty()) {
						break;
					}
					Record record = recv.get();
					// see RecordWriter
					Iterator<Value> iterator = record.values().iterator();
					while (iterator.hasNext()) {
						Value value = iterator.next();
						value.append(writer, locale);
						if (iterator.hasNext()) {
							writer.append(' ');
						}
					}
					writer.append(System.lineSeparator());
				}
				return ExitStatus.success();
			} catch (IOException e) {
				err.send(Record.of(Keys.ERROR, Values.ofText(e.getMessage())));
				return ExitStatus.error();
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
