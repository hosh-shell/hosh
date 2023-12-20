/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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
package hosh;

import hosh.runtime.AutoTableChannel;
import hosh.runtime.BootstrapBuiltins;
import hosh.runtime.CancellableChannel;
import hosh.runtime.CommandCompleter;
import hosh.runtime.CommandResolver;
import hosh.runtime.CommandResolvers;
import hosh.runtime.Compiler;
import hosh.runtime.Compiler.Program;
import hosh.runtime.ConsoleChannel;
import hosh.runtime.DisabledHistory;
import hosh.runtime.FileSystemCompleter;
import hosh.runtime.HoshFormatter;
import hosh.runtime.HoshHighlighter;
import hosh.runtime.Injector;
import hosh.runtime.Interpreter;
import hosh.runtime.PathInitializer;
import hosh.runtime.Prompt;
import hosh.runtime.ReplReader;
import hosh.runtime.VariableExpansionCompleter;
import hosh.runtime.VersionLoader;
import hosh.runtime.MutableState;
import hosh.spi.Ansi;
import hosh.spi.Command;
import hosh.spi.Errors;
import hosh.spi.ExitStatus;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.spi.Version;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Main class
 */
public class Hosh {

	/**
	 * OS environment variables supported by Hosh.
	 */
	public static class Environment {

		private Environment() {
		}

		/**
		 * Allows configuring default log level.
		 * <p>
		 * Allowed values: as @{@link Level#parse(String)}.
		 * Default: null, meaning that logging is disabled.
		 */
		public static final String HOSH_LOG_LEVEL = "HOSH_LOG_LEVEL";

		/**
		 * Allows enabling/disabling history.
		 * <p>
		 * Allowed values: "true", "false".
		 * Default: "true".
		 */
		public static final String HOSH_HISTORY = "HOSH_HISTORY";

	}

	private Hosh() {
	}

	public static void main(String[] args) throws Exception {
		Version version = VersionLoader.loadVersion();
		configureLogging();
		Logger logger = LoggerFactory.forEnclosingClass();

		logger.info(() -> String.format("starting hosh %s", version));
		ExitStatus exitStatus;
		try (Terminal terminal = TerminalBuilder.builder().jna(true).jansi(false).exec(false).build()) {
			exitStatus = run(terminal, version, logger, args);
		}
		System.exit(exitStatus.value());
	}

	// enabling logging to $HOME/.hosh.log
	// if and only if HOSH_LOG_LEVEL is defined
	// by default logging is disabled
	@SuppressWarnings("squid:S4792")
	private static void configureLogging() throws IOException {
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		String logLevel = System.getenv(Environment.HOSH_LOG_LEVEL);
		if (logLevel == null) {
			return;
		}
		HoshFormatter formatter = new HoshFormatter();
		String homeDir = System.getProperty("user.home", "");
		String logFilePath = new File(homeDir, ".hosh.log").getAbsolutePath();
		FileHandler fileHandler = new FileHandler(logFilePath);
		fileHandler.setFormatter(formatter);
		String rootLoggerName = "";
		Logger logger = logManager.getLogger(rootLoggerName);
		logger.addHandler(fileHandler);
		logger.setLevel(Level.parse(logLevel));
	}

	private static ExitStatus run(Terminal terminal, Version version, Logger logger, String[] args) {
		Map<String, Supplier<Command>> commands = new BootstrapBuiltins().registerAllBuiltins();
		MutableState state = new MutableState();
		state.mutateCwd(Paths.get("."));
		state.mutateCommands(commands);
		state.mutateVariables(System.getenv());
		state.mutatePath(new PathInitializer().initializePath(System.getenv("PATH")));
		Injector injector = new Injector();
		injector.setLineReader(LineReaderBuilder.builder().terminal(terminal).build());
		injector.setState(state);
		injector.setStateMutator(state);
		injector.setTerminal(terminal);
		CommandResolver commandResolver = CommandResolvers.builtinsThenExternal(state);
		Compiler compiler = new Compiler(commandResolver);
		OutputChannel out = new CancellableChannel(new ConsoleChannel(terminal, Ansi.Style.NONE));
		OutputChannel err = new CancellableChannel(new ConsoleChannel(terminal, Ansi.Style.FG_RED));
		Interpreter interpreter = new Interpreter(state, state, injector);
		CommandLine commandLine;
		Options options = createOptions();
		CommandLineParser parser = new DefaultParser();
		try {
			commandLine = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("hosh: " + e.getMessage());
			return ExitStatus.error();
		}
		if (commandLine.hasOption('h')) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("hosh", options);
			return ExitStatus.success();
		}
		if (commandLine.hasOption('v')) {
			System.out.println(version.hoshVersion().unwrap(String.class).orElseThrow());
			return ExitStatus.success();
		}
		List<String> remainingArgs = commandLine.getArgList();
		if (remainingArgs.isEmpty()) {
			welcome(out, version);
			return repl(state, terminal, compiler, interpreter, injector, out, err, logger);
		}
		if (remainingArgs.size() == 1) {
			String filePath = args[0];
			return script(filePath, compiler, interpreter, injector, out, err, logger);
		}
		System.err.println("hosh: too many scripts");
		return ExitStatus.error();
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption("h", "help", false, "show help and exit");
		options.addOption("v", "version", false, "show version and exit");
		return options;
	}

	private static ExitStatus script(String path, Compiler compiler, Interpreter interpreter, Injector injector, OutputChannel out, OutputChannel err, Logger logger) {
		try {
			String script = loadScript(Paths.get(path));
			Program program = compiler.compile(script);
			injector.setHistory(new DisabledHistory());
			return interpreter.eval(program, out, err);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "caught exception", e);
			err.send(Errors.message(e));
			return ExitStatus.error();
		}
	}

	private static String loadScript(Path path) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("unable to load: " + path, e);
		}
	}

	private static ExitStatus repl(State state, Terminal terminal, Compiler compiler, Interpreter interpreter, Injector injector,
								   OutputChannel out, OutputChannel err, Logger logger) {
		History history = provideHistory();
		injector.setHistory(history);
		LineReader lineReader = LineReaderBuilder
			.builder()
			.appName("hosh")
			.history(history)
			.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh_history"))
			.completer(new AggregateCompleter(
				new CommandCompleter(state),
				new FileSystemCompleter(state),
				new VariableExpansionCompleter(state)))
			.terminal(terminal)
			.highlighter(new HoshHighlighter(compiler))
			.build();
		Prompt prompt = new Prompt();
		ReplReader reader = new ReplReader(prompt, lineReader);
		AutoTableChannel autoTableChannel = new AutoTableChannel(out);
		while (true) {
			Optional<String> line = reader.read();
			if (line.isEmpty()) {
				break;
			}
			try {
				Program program = compiler.compile(line.get());
				ExitStatus exitStatus = interpreter.eval(program, autoTableChannel, err);
				autoTableChannel.end();
				if (state.isExit()) {
					return exitStatus;
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, String.format("caught exception for input: '%s'", line.get()), e);
				err.send(Errors.message(e));
			}
		}
		return ExitStatus.success();
	}

	private static History provideHistory() {
		String historyEnabled = System.getenv().getOrDefault(Environment.HOSH_HISTORY, "true");
		if (Boolean.parseBoolean(historyEnabled)) {
			return new DefaultHistory();
		} else {
			return new DisabledHistory();
		}
	}

	private static void welcome(OutputChannel out, Version version) {
		out.send(Records.singleton(Keys.TEXT, version.hoshVersion()));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Running on Java " + System.getProperty("java.version"))));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("PID is " + ProcessHandle.current().pid())));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Locale is " + Locale.getDefault().toString())));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Timezone is " + TimeZone.getDefault().getID())));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Encoding is " + Charset.defaultCharset().displayName())));
		out.send(Records.singleton(Keys.TEXT, Values.ofText("Use 'exit' or Ctrl-D (i.e. EOF) to exit")));
	}
}
