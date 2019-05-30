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
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hosh.doc.Todo;
import org.hosh.runtime.AnsiFormatter;
import org.hosh.runtime.CancellableChannel;
import org.hosh.runtime.CommandCompleter;
import org.hosh.runtime.CommandResolver;
import org.hosh.runtime.CommandResolvers;
import org.hosh.runtime.Compiler;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.ConsoleChannel;
import org.hosh.runtime.FileSystemCompleter;
import org.hosh.runtime.Interpreter;
import org.hosh.runtime.Prompt;
import org.hosh.runtime.ReplReader;
import org.hosh.runtime.VariableExpansionCompleter;
import org.hosh.runtime.VersionLoader;
import org.hosh.spi.Ansi;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Records;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/** Main class */
public class Hosh {

	private Hosh() {
	}

	public static void main(String[] args) throws Exception {
		configureLogging();
		Logger logger = LoggerFactory.forEnclosingClass();
		String version = VersionLoader.loadVersion();
		logger.info(() -> String.format("starting hosh v.%s", version));
		ExitStatus exitStatus = ExitStatus.error();
		try (Terminal terminal = TerminalBuilder.builder().exec(false).jna(true).build()) {
			exitStatus = run(terminal, version, logger, args);
		}
		System.exit(exitStatus.value());
	}

	// enabling logging to $HOME/.hosh.log
	// if and only if HOSH_LOG_LEVEL is defined
	// by default logging is disabled
	private static void configureLogging() throws IOException {
		String homeDir = System.getProperty("user.home", "");
		String logLevel = Objects.toString(System.getenv("HOSH_LOG_LEVEL"), "OFF");
		String logFilePath = new File(homeDir, ".hosh.log").getAbsolutePath();
		AnsiFormatter formatter = new AnsiFormatter();
		FileHandler fileHandler = new FileHandler(logFilePath);
		fileHandler.setFormatter(formatter);
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		String rootLoggerName = "";
		Logger logger = logManager.getLogger(rootLoggerName);
		logger.addHandler(fileHandler);
		logger.setLevel(Level.parse(logLevel));
	}

	private static ExitStatus run(Terminal terminal, String version, Logger logger, String[] args) {
		List<Path> path = Stream
				.of(System.getenv("PATH").split(File.pathSeparator))
				.map(Paths::get)
				.collect(Collectors.toList());
		State state = new State();
		state.setCwd(Paths.get("."));
		state.getVariables().putAll(System.getenv());
		state.setPath(path);
		BootstrapBuiltins bootstrap = new BootstrapBuiltins();
		bootstrap.registerAllBuiltins(state);
		CommandResolver commandResolver = CommandResolvers.builtinsThenExternal(state);
		Compiler compiler = new Compiler(commandResolver);
		Channel out = new CancellableChannel(new ConsoleChannel(terminal, Ansi.Style.NONE));
		Channel err = new CancellableChannel(new ConsoleChannel(terminal, Ansi.Style.FG_RED));
		Interpreter interpreter = new Interpreter(state, terminal, out, err);
		ExitStatus exitStatus;
		if (args.length == 0) {
			welcome(out, version);
			exitStatus = repl(state, terminal, compiler, interpreter, err, logger);
		} else {
			String filePath = args[0];
			exitStatus = script(filePath, compiler, interpreter, err, logger);
		}
		return exitStatus;
	}

	private static ExitStatus script(String path, Compiler compiler, Interpreter interpreter, Channel err, Logger logger) {
		try {
			String script = loadScript(Paths.get(path));
			Program program = compiler.compile(script);
			return interpreter.eval(program);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "caught exception", e);
			err.send(Records.singleton(Keys.ERROR, Values.ofText(Objects.toString(e.getMessage(), "(no message)"))));
			return ExitStatus.error();
		}
	}

	@Todo(description = "move to the right place (parser/lexer), this is just proof of concept")
	private static String loadScript(Path path) {
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			return lines
					.map(line -> line.trim().endsWith("|") ? line : line + ";")
					.collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException("unable to load: " + path, e);
		}
	}

	private static ExitStatus repl(State state, Terminal terminal, Compiler compiler, Interpreter interpreter,
			Channel err, Logger logger) {
		LineReader lineReader = LineReaderBuilder
				.builder()
				.appName("hosh")
				.history(new DefaultHistory())
				.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
				.variable(LineReader.HISTORY_FILE_SIZE, "1000")
				.completer(new AggregateCompleter(
						new CommandCompleter(state),
						new FileSystemCompleter(state),
						new VariableExpansionCompleter(state)))
				.terminal(terminal)
				.build();
		Prompt prompt = new Prompt();
		ReplReader reader = new ReplReader(prompt, lineReader);
		while (true) {
			Optional<String> line = reader.read();
			if (line.isEmpty()) {
				break;
			}
			try {
				Program program = compiler.compile(line.get());
				ExitStatus exitStatus = interpreter.eval(program);
				if (state.isExit()) {
					return exitStatus;
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, String.format("caught exception for input: '%s'", line.get()), e);
				err.send(Records.singleton(Keys.ERROR, Values.ofText(Objects.toString(e.getMessage(), "(no message)"))));
			}
		}
		return ExitStatus.success();
	}

	private static void welcome(Channel out, String version) {
		out.send(Records.singleton(Keys.TEXT, Values.ofText("hosh " + version)));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Running on Java " + System.getProperty("java.version"))));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("PID is " + ProcessHandle.current().pid())));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Locale is " + Locale.getDefault().toString())));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Use 'exit' or Ctrl-D (i.e. EOF) to exit")));
	}
}
