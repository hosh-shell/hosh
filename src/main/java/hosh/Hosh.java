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
package hosh;

import hosh.doc.Todo;
import hosh.runtime.HoshFormatter;
import hosh.runtime.Prompt;
import hosh.runtime.ReplReader;
import hosh.runtime.VersionLoader;
import hosh.script.HoshScriptEngine;
import hosh.spi.ExitStatus;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jline.reader.LineReader;

/**
 * Main class
 */
public class Hosh {

	private Hosh() {
	}

	public static void main(String[] args) throws Exception {
		configureLogging();
		ExitStatus exitStatus = run(args);
		System.exit(exitStatus.value());
	}

	// enabling logging to $HOME/.hosh.log
	// if and only if HOSH_LOG_LEVEL is defined
	// by default logging is disabled
	private static void configureLogging() throws IOException {
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		String logLevel = System.getenv("HOSH_LOG_LEVEL");
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

	private static ExitStatus run(String[] args) throws IOException, ScriptException {
		String version = VersionLoader.loadVersion();
		Options options = createOptions();
		CommandLineParser parser = new DefaultParser();
		CommandLine commandLine;
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
			System.out.println("hosh " + version);
			return ExitStatus.success();
		}
		HoshScriptEngine hoshScriptEngine = (HoshScriptEngine) new ScriptEngineManager().getEngineByName("hosh");
		List<String> remainingArgs = commandLine.getArgList();
		if (remainingArgs.isEmpty()) {
			welcome(version);
			return repl(hoshScriptEngine);
		}
		if (remainingArgs.size() == 1) {
			String filePath = args[0];
			return script(hoshScriptEngine, filePath);
		}
		System.err.println("hosh: too many scripts");
		return ExitStatus.error();
	}

	private static ExitStatus script(HoshScriptEngine hoshScriptEngine, String filePath) throws ScriptException {
		String script = loadScript(Paths.get(filePath));
		return (ExitStatus) hoshScriptEngine.eval(script);
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption("h", "help", false, "show help and exit");
		options.addOption("v", "version", false, "show version and exit");
		return options;
	}

	private static String loadScript(Path path) {
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			return lines
					.map(line -> line.trim().endsWith("|") ? line : line + ";")
					.collect(Collectors.joining("\n"));
		} catch (IOException e) {
			throw new UncheckedIOException("unable to load: " + path, e);
		}
	}

	@Todo(description = "no history")
	private static ExitStatus repl(HoshScriptEngine hoshScriptEngine) throws ScriptException {
		LineReader lineReader = hoshScriptEngine.getContext().createLineReader();
		Prompt prompt = new Prompt();
		ReplReader reader = new ReplReader(prompt, lineReader);
		while (true) {
			Optional<String> line = reader.read();
			if (line.isEmpty()) {
				break;
			}
			ExitStatus exitStatus = (ExitStatus) hoshScriptEngine.eval(line.get());
			if (hoshScriptEngine.getContext().isExit()) {
				return exitStatus;
			}
		}
		return ExitStatus.success();
	}

	private static void welcome(String version) {
		System.out.println("hosh " + version);
		System.out.println("Running on Java " + System.getProperty("java.version"));
		System.out.println("PID is " + ProcessHandle.current().pid());
		System.out.println("Locale is " + Locale.getDefault().toString());
		System.out.println("Timezone is " + TimeZone.getDefault().getID());
		System.out.println("Encoding is " + System.getProperty("file.encoding"));
		System.out.println("Use 'exit' or Ctrl-D (i.e. EOF) to exit");
	}
}
