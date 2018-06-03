package org.hosh;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ServiceLoader;

import org.hosh.runtime.CommandCompleter;
import org.hosh.runtime.CommandFactory;
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
import org.hosh.spi.State;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStyle;

public class Main {

	public static void main(String[] args) throws Exception {
		State state = new State();
		Terminal terminal = TerminalBuilder.terminal();
		LineReader lineReader = LineReaderBuilder.builder().appName("hosh").terminal(terminal)
				.history(new DefaultHistory())
				.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
				.variable(LineReader.HISTORY_FILE_SIZE, "1000").completer(new CommandCompleter(state)).build();
		CommandRegistry commandRegistry = new SimpleCommandRegistry(state);
		ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
		for (Module module : modules) {
			module.onStartup(commandRegistry);
		}
		CommandFactory commandFactory = new CommandFactory(state, terminal);
		Compiler compiler = new Compiler(state, commandFactory);
		Channel out = new ConsoleChannel(terminal, AttributedStyle.DEFAULT);
		Channel err = new ConsoleChannel(terminal, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
		Interpreter interpreter = new Interpreter(out, err);
		welcome(out);
		repl(lineReader, compiler, interpreter, out, err);
	}

	private static void repl(LineReader lineReader, Compiler compiler, Interpreter interpreter, Channel out,
			Channel err) {
		LineReaderIterator lineIterator = new LineReaderIterator(lineReader);
		while (lineIterator.hasNext()) {
			String line = lineIterator.next();
			try {
				Program program = compiler.compile(line);
				interpreter.eval(program);
			} catch (RuntimeException e) {
				// TODO: log exception
				e.printStackTrace();
				err.send(Record.empty().add("message", e));
			}
		}
	}

	private static void welcome(Channel out) throws IOException {
		out.send(Record.empty().add("message", "hosh v" + Version.readVersion()));
		out.send(Record.empty().add("message", "Running on Java " + System.getProperty("java.version")));
	}

}
