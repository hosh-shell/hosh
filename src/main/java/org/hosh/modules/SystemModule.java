/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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

import java.lang.ProcessHandle.Info;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
	}

	public static class Echo implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			Record record = Record.of("text", Values.ofText(String.join("", args)));
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
				err.send(Record.of("error", Values.ofText("expecting no parameters")));
				return ExitStatus.error();
			}
			Map<String, String> env = state.getVariables();
			for (Map.Entry<String, String> entry : env.entrySet()) {
				Record record = Record.of(
						"key", Values.ofText(entry.getKey()),
						"value", Values.ofText(entry.getValue()));
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
						err.send(Record.of("error", Values.ofText("not a valid exit status: " + arg)));
						return ExitStatus.error();
					}
				default:
					err.send(Record.of("error", Values.ofText("too many parameters")));
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

	public static class Sleep implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
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
		@Override
		public Long before(List<String> args, Channel in, Channel out, Channel err) {
			return System.nanoTime();
		}

		@Override
		public void after(Long startNanos, Channel in, Channel out, Channel err) {
			long endNanos = System.nanoTime();
			Duration duration = Duration.ofNanos(endNanos - startNanos);
			out.send(Record.of("message", Values.ofDuration(duration)));
		}
	}

	public static class ProcessList implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of("error", Values.ofText("expecting zero arguments")));
				return ExitStatus.error();
			}
			ProcessHandle.allProcesses().forEach(process -> {
				Info info = process.info();
				Record result = Record.empty()
						.append("pid", Values.ofNumeric(process.pid()))
						.append("user", Values.ofText(info.user().orElse("-")))
						.append("start", Values.ofText(info.startInstant().map(Instant::toString).orElse("-")))
						.append("command", Values.ofText(info.command().orElse("-")))
						.append("arguments", Values.ofText(String.join(" ", info.arguments().orElse(new String[0]))));
				out.send(result);
			});
			return ExitStatus.success();
		}
	}

	public static class KillProcess implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of("error", Values.ofText("expecting one argument")));
				return ExitStatus.error();
			}
			try {
				long pid = Long.parseLong(args.get(0));
				Optional<ProcessHandle> process = ProcessHandle.of(pid);
				if (process.isEmpty()) {
					err.send(Record.of("error", Values.ofText("cannot find pid: " + pid)));
					return ExitStatus.error();
				}
				boolean destroyed = process.get().destroy();
				if (!destroyed) {
					err.send(Record.of("error", Values.ofText("cannot destroy pid: " + pid)));
					return ExitStatus.error();
				}
				return ExitStatus.success();
			} catch (NumberFormatException e) {
				err.send(Record.of("error", Values.ofText("not a valid pid: " + args.get(0))));
				return ExitStatus.error();
			}
		}
	}

	public static class Err implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			simulateNullPointer();
			return ExitStatus.success();
		}

		private void simulateNullPointer() {
			throw new NullPointerException("injected error: please do not report");
		}
	}

	public static class Benchmark implements CommandWrapper<SystemModule.Benchmark.Accumulator> {
		@Override
		public Accumulator before(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				throw new IllegalArgumentException("requires one integer arg");
			}
			Accumulator accumulator = new Accumulator();
			int repeat = Integer.parseInt(args.get(0));
			if (repeat <= 0) {
				throw new IllegalArgumentException("repeat should be > 0");
			}
			accumulator.repeat = repeat;
			accumulator.results = new ArrayList<>(repeat);
			accumulator.start();
			return accumulator;
		}

		@Override
		public void after(Accumulator resource, Channel in, Channel out, Channel err) {
			Duration best = resource.results.stream().min(Comparator.naturalOrder()).orElse(Duration.ZERO);
			Duration worst = resource.results.stream().max(Comparator.naturalOrder()).orElse(Duration.ZERO);
			int runs = resource.results.size();
			Duration avg = runs == 0 ? Duration.ZERO : resource.results.stream().reduce(Duration.ZERO, (acc, d) -> acc.plus(d)).dividedBy(runs);
			out.send(Record.of("count", Values.ofNumeric(runs))
					.append("best", Values.ofDuration(best))
					.append("worst", Values.ofDuration(worst))
					.append("avg", Values.ofDuration(avg)));
		}

		@Override
		public boolean retry(Accumulator resource) {
			resource.takeTime();
			return --resource.repeat > 0;
		}

		public static class Accumulator {
			private List<Duration> results;
			private int repeat;
			private long nanoTime;

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
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			while (true) {
				out.send(Record.of("source", Values.none()));
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
}
