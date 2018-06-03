package org.hosh;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ServiceLoader;

import org.hosh.runtime.CommandCompleter;
import org.hosh.runtime.CommandFactory;
import org.hosh.runtime.ConsoleChannel;
import org.hosh.runtime.Evaluator;
import org.hosh.runtime.LineReaderIterator;
import org.hosh.runtime.SimpleCommandRegistry;
import org.hosh.runtime.Version;
import org.hosh.spi.Channel;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.State;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

public class Main {

	public static void main(String[] args) throws IOException {
		State state = new State();
		Terminal terminal = TerminalBuilder.terminal();
		LineReader lineReader = LineReaderBuilder.builder().appName("hosh").terminal(terminal)
				.history(new DefaultHistory())
				.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
				.variable(LineReader.HISTORY_FILE_SIZE, "1000").completer(new CommandCompleter(state))
				.build();
		CommandRegistry commandRegistry = new SimpleCommandRegistry(state);
		ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
		for (Module module : modules) {
			module.onStartup(commandRegistry);
		}
		CommandFactory commandFactory = new CommandFactory(state, terminal);
		Evaluator evaluator = new Evaluator(state, commandFactory);
		repl(evaluator, lineReader, terminal);
	}

	private static void repl(Evaluator evaluator, LineReader lineReader, Terminal terminal) throws IOException {
		showVersion(terminal);
		Channel out = new ConsoleChannel(terminal);
		Channel err = new ConsoleChannel(terminal);
		LineReaderIterator lineIterator = new LineReaderIterator(lineReader);
		while (lineIterator.hasNext()) {
			String line = lineIterator.next();
			try {
				evaluator.run(line, out, err);
			} catch (RuntimeException e) {
				writeRed(terminal, e.getMessage());
			}
		}
	}

	private static void writeGreen(Terminal terminal, String string) {
		terminal.writer().println(AttributedString.fromAnsi("\u001B[32m" + string + "\u001B[0m").toAnsi(terminal));
	}

	private static void writeRed(Terminal terminal, String string) {
		terminal.writer().println(AttributedString.fromAnsi("\u001B[31m" + string + "\u001B[0m").toAnsi(terminal));
	}

	private static void showVersion(Terminal terminal) throws IOException {
		writeGreen(terminal, "hosh v" + Version.readVersion());
		writeGreen(terminal, "Running on Java " + System.getProperty("java.version"));
		terminal.flush();
	}

}
