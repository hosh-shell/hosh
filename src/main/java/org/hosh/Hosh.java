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
package org.hosh;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hosh.runtime.Ansi;
import org.hosh.runtime.CommandCompleter;
import org.hosh.runtime.CommandResolver;
import org.hosh.runtime.CommandResolvers;
import org.hosh.runtime.Compiler;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.ConsoleChannel;
import org.hosh.runtime.FileSystemCompleter;
import org.hosh.runtime.Interpreter;
import org.hosh.runtime.LineReaderIterator;
import org.hosh.runtime.SimpleChannel;
import org.hosh.runtime.SimpleCommandRegistry;
import org.hosh.runtime.Version;
import org.hosh.spi.Channel;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/** Main class */
public class Hosh {
	private Hosh() {
	}

	// enabling logging to $HOME/.hosh.log
	// if and only if HOSH_LOG_LEVEL is defined (i.e. DEBUG)
	// by default logging is disabled
	private static void configureLogging() {
		String homeDir = System.getProperty("user.home", "");
		String logFilePath = new File(homeDir, ".hosh.log").getAbsolutePath();
		String logLevel = Objects.toString(System.getenv("HOSH_LOG_LEVEL"), "OFF");
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, logLevel);
		System.setProperty(org.slf4j.impl.SimpleLogger.LOG_FILE_KEY, logFilePath);
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	public static void main(String[] args) throws Exception {
		configureLogging();
		Logger logger = LoggerFactory.getLogger(Hosh.class);
		try (Terminal terminal = TerminalBuilder.builder().build()) {
			runWithin(terminal, logger, args);
		}
	}

	private static void runWithin(Terminal terminal, Logger logger, String[] args) throws IOException {
		String version = Version.readVersion();
		logger.info("starting hosh {}", version);
		List<Path> path = Stream
				.of(System.getenv("PATH").split(File.pathSeparator))
				.map(Paths::get)
				.collect(Collectors.toList());
		State state = new State();
		state.setId(1);
		state.setCwd(Paths.get("."));
		state.getVariables().putAll(System.getenv());
		state.setPath(path);
		CommandRegistry commandRegistry = new SimpleCommandRegistry(state);
		ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
		for (Module module : modules) {
			module.onStartup(commandRegistry);
		}
		CommandResolver commandResolver = CommandResolvers.builtinsThenSystem(state);
		Compiler compiler = new Compiler(commandResolver);
		if (args.length == 0) {
			LineReader lineReader = LineReaderBuilder
					.builder()
					.appName("hosh")
					.history(new DefaultHistory())
					.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
					.variable(LineReader.HISTORY_FILE_SIZE, "1000")
					.completer(new AggregateCompleter(new CommandCompleter(state), new FileSystemCompleter(state)))
					.terminal(terminal)
					.build();
			Channel out = new ConsoleChannel(terminal, Ansi.Style.NONE);
			Channel err = new ConsoleChannel(terminal, Ansi.Style.FG_RED);
			Interpreter interpreter = new Interpreter(state, terminal, out, err);
			welcome(out, version);
			repl(state, lineReader, compiler, interpreter, err, logger);
		} else {
			Channel out = new SimpleChannel(System.out);
			Channel err = new SimpleChannel(System.err);
			Interpreter interpreter = new Interpreter(state, terminal, out, err);
			String filePath = args[0];
			script(filePath, compiler, interpreter, err, logger);
		}
	}

	private static void script(String path, Compiler compiler, Interpreter interpreter, Channel err, Logger logger) {
		try {
			String script = loadScript(Paths.get(path));
			Program program = compiler.compile(script);
			ExitStatus exitStatus = interpreter.eval(program);
			System.exit(exitStatus.value());
		} catch (Exception e) {
			logger.error("caught exception", e);
			err.send(Record.of("message", Values.ofText(e.getMessage())));
			System.exit(1);
		}
	}

	private static String loadScript(Path path) {
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			return lines.collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException("unable to load: " + path, e);
		}
	}

	private static void repl(State state, LineReader lineReader, Compiler compiler, Interpreter interpreter,
			Channel err, Logger logger) {
		LineReaderIterator read = new LineReaderIterator(state, lineReader);
		while (read.hasNext()) {
			state.setId(state.getId() + 1);
			String line = read.next();
			try {
				Program program = compiler.compile(line);
				ExitStatus exitStatus = interpreter.eval(program);
				if (state.isExit()) {
					System.exit(exitStatus.value());
				}
			} catch (Exception e) {
				logger.error("caught exception for input: '{}'", line, e);
				err.send(Record.of("message", Values.ofText(e.getMessage())));
			}
		}
		System.exit(0);
	}

	private static void welcome(Channel out, String version) {
		out.send(Record.of("message", Values.ofText("hosh v" + version)));
		out.send(Record.of("message", Values.ofText("Running on Java " + System.getProperty("java.version"))));
		out.send(Record.of("message", Values.ofText("PID is " + ProcessHandle.current().pid())));
		out.send(Record.of("message", Values.ofText("Locale is " + Locale.getDefault().toString())));
		out.send(Record.of("message", Values.ofText("Use 'exit' or Ctrl-D (i.e. EOF) to exit")));
	}
}
