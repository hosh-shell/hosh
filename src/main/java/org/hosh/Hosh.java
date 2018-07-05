package org.hosh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hosh.runtime.CommandCompleter;
import org.hosh.runtime.Compiler;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.ConsoleChannel;
import org.hosh.runtime.Interpreter;
import org.hosh.runtime.LineReaderIterator;
import org.hosh.runtime.SimpleCommandRegistry;
import org.hosh.runtime.Version;
import org.hosh.spi.Channel;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.SimpleChannel;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/** Main class */
public class Hosh {

	private Hosh() {
	}

	public static void main(String[] args) throws Exception {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		Logger logger = LoggerFactory.getLogger(Hosh.class);
		String version = Version.readVersion();
		logger.info("starting hosh {}", version);
		Terminal terminal = TerminalBuilder
				.builder()
				.system(true)
				.build();
		State state = new State();
		state.setId(1);
		state.setCwd(Paths.get("."));
		LineReader lineReader = LineReaderBuilder
				.builder()
				.appName("hosh")
				.history(new DefaultHistory())
				.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
				.variable(LineReader.HISTORY_FILE_SIZE, "1000")
				.completer(new CommandCompleter(state))
				.terminal(terminal)
				.build();
		CommandRegistry commandRegistry = new SimpleCommandRegistry(state);
		ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
		for (Module module : modules) {
			module.onStartup(commandRegistry);
		}
		Compiler compiler = new Compiler(state);
		if (args.length == 0) {
			Channel out = new ConsoleChannel(terminal, AttributedStyle.WHITE);
			Channel err = new ConsoleChannel(terminal, AttributedStyle.RED);
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

	private static void script(String path, Compiler compiler, Interpreter interpreter, Channel err, Logger logger)
			throws IOException {
		try (Stream<String> lines = Files.lines(Paths.get(path), StandardCharsets.UTF_8)) {
			String script = lines.collect(Collectors.joining("\n"));
			Program program = compiler.compile(script);
			interpreter.eval(program);
			System.exit(0);
		} catch (IOException e) {
			err.send(Record.of("message", Values.ofText("unable to load: " + path)));
			logger.debug("caught exception for load: " + path, e);
			System.exit(1);
		} catch (Exception e) {
			logger.debug("caught exception", e);
			err.send(Record.of("message", Values.ofText(e.getMessage())));
			System.exit(1);
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
				interpreter.eval(program);
			} catch (RuntimeException e) {
				logger.debug("caught exception for input: " + line, e);
				err.send(Record.of("message", Values.ofText(e.getMessage())));
			}
		}
		System.exit(0);
	}

	private static void welcome(Channel out, String version) {
		out.send(Record.of("message", Values.ofText("hosh v" + version)));
		out.send(Record.of("message", Values.ofText("Running on Java " + System.getProperty("java.version"))));
	}

}
