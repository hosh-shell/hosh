package org.hosh;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hosh.antlr4.HoshParser;
import org.hosh.runtime.*;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.State;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class Main {

	public static void main(String[] args) throws IOException {
		System.setProperty("picocli.ansi", "false");
		Terminal terminal = TerminalBuilder.terminal();
		try {
			Options options = CommandLine.populateCommand(new Options(), args);
			if (options.helpRequested) {
				CommandLine.usage(options, System.out); // TODO: use ANSI codes
				System.exit(0);
			}
			if (options.version) {
				showVersion(terminal);
				System.exit(0);
			}
		} catch (CommandLine.PicocliException e) {
			writeRed(terminal, e.getMessage());
			System.exit(1);
		}
		State state = new State();
		CommandFactory commandFactory = new CommandFactory(terminal, state);
		CommandRegistry commandRegistry = new SimpleCommandRegistry(commandFactory);
		LineReader lineReader = LineReaderBuilder.builder().appName("hosh").terminal(terminal)
				.history(new DefaultHistory())
				.variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
				.variable(LineReader.HISTORY_FILE_SIZE, "1000").completer(new CommandCompleter(commandRegistry))
				.build();
		ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
		for (Module module : modules) {
			module.onStartup(commandRegistry);
		}
		repl(commandRegistry, lineReader, terminal);
	}

	private static void repl(CommandRegistry commandRegistry, LineReader lineReader, Terminal terminal)
			throws IOException {
		showVersion(terminal);
		LineReaderIterator lineIterator = new LineReaderIterator(lineReader);
		while (lineIterator.hasNext()) {
			String line = lineIterator.next();
			executeLine(commandRegistry, line, terminal);
		}
	}

	private static void executeLine(CommandRegistry commandRegistry, String line, Terminal terminal) {
		try {
			HoshParser.ProgramContext programContext = Parser.parse(line + '\n');
			programContext.stmt().forEach(stmt -> {
				String commandName = stmt.ID().get(0).getSymbol().getText();
				Optional<Command> search = commandRegistry.search(commandName);
				if (search.isPresent()) {
					Channel out = new ConsoleChannel(System.out);
					Channel err = new ConsoleChannel(System.err);
					List<String> commandArgs = stmt.ID().stream().skip(1).map(TerminalNode::getSymbol)
							.map(Token::getText).collect(Collectors.toList());
					search.get().run(commandArgs, out, err);
				} else {
					writeRed(terminal, "no such commnad");
				}
			});
		} catch (Parser.ParseError e) {
			writeRed(terminal, e.getMessage());
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

	public static class Options {

		@CommandLine.Option(names = { "-v", "--version" }, description = "print version information and exit")
		boolean version;

		@CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
		private boolean helpRequested = false;

	}

}
