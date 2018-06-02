package org.hosh;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hosh.antlr4.HoshParser;
import org.hosh.runtime.*;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class Main {

	public static void main(String[] args) throws IOException {
		try {
			Options options = CommandLine.populateCommand(new Options(), args);
			if (options.helpRequested) {
				CommandLine.usage(options, System.out);
				System.exit(0);
			}
			if (options.version) {
				showVersion(System.out);
				System.exit(0);
			}
		} catch (CommandLine.PicocliException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		Terminal terminal = TerminalBuilder.terminal();
		CommandFactory commandFactory = new CommandFactory(terminal);
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
		repl(commandRegistry, lineReader);
	}

	private static void repl(CommandRegistry commandRegistry, LineReader lineReader) throws IOException {
		showVersion(System.out);
		LineReaderIterator lineIterator = new LineReaderIterator(lineReader);
		while (lineIterator.hasNext()) {
			String line = lineIterator.next();
			executeLine(commandRegistry, line);
		}
	}

	private static void executeLine(CommandRegistry commandRegistry, String line) {
		try {
			HoshParser.ProgramContext programContext = Parser.parse(line + '\n');
			programContext.stmt().forEach(stmt -> {
				String commandName = stmt.ID().get(0).getSymbol().getText();
				Optional<Command> search = commandRegistry.search(commandName);
				if (search.isPresent()) {
					List<String> commandArgs = stmt.ID().stream().skip(1).map(TerminalNode::getSymbol)
							.map(Token::getText).collect(Collectors.toList());
					search.get().run(commandArgs);
				} else {
					System.err.println("command not found");
				}
			});
		} catch (Parser.ParseError e) {
			System.err.println(e.getMessage());
		}
	}

	private static void showVersion(PrintStream printStream) throws IOException {
		printStream.println("hosh v" + Version.readVersion());
		printStream.println("Running on Java " + System.getProperty("java.version"));
	}

	public static class Options {

		@CommandLine.Option(names = { "-v", "--version" }, description = "print version information and exit")
		boolean version;

		@CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
		private boolean helpRequested = false;

	}

}
