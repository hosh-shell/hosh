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
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	// TODO: configure logger to log under hidden home directory
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		String prompt = new AttributedStringBuilder().style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
				.append("hosh> ").style(AttributedStyle.DEFAULT).toAnsi();
		State state = new State();
		state.setPrompt(prompt);
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
		LineReaderIterator read = new LineReaderIterator(state, lineReader);
		CommandFactory commandFactory = new CommandFactory(state, terminal);
		Compiler compiler = new Compiler(state, commandFactory);
		Channel out = new ConsoleChannel(terminal, AttributedStyle.WHITE);
		Channel err = new ConsoleChannel(terminal, AttributedStyle.RED); 
		Interpreter interpreter = new Interpreter(out, err);
		welcome(out);
		repl(read, compiler, interpreter, err);
	}

	private static void repl(LineReaderIterator read, Compiler compiler, Interpreter interpreter, Channel err) {
		while (read.hasNext()) {
			String line = read.next();
			try {
				Program program = compiler.compile(line);
				interpreter.eval(program);
			} catch (RuntimeException e) {
				logger.debug("caught exception for input: " + line, e);
				err.send(Record.of("message", e.getMessage()));
			}
		}
	}

	private static void welcome(Channel out) throws IOException {
		out.send(Record.of("message", "hosh v" + Version.readVersion()));
		out.send(Record.of("message", "Running on Java " + System.getProperty("java.version")));
	}

}
