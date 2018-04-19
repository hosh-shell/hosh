package org.hosh;

import org.hosh.antlr4.HoshParser;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ServiceLoader;

public class Main {

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.terminal();
        CommandFactory commandFactory = new CommandFactory(terminal);
        CommandRegistry commandRegistry = new SimpleCommandRegistry(commandFactory);
        LineReader lineReader = LineReaderBuilder.builder()
                .appName("hosh")
                .terminal(terminal)
                .history(new DefaultHistory())
                .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".hosh.history"))
                .variable(LineReader.HISTORY_FILE_SIZE, "1000")
                .completer(new CommandCompleter(commandRegistry))
                .build();
        ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
        for (Module module : modules) {
            module.onStartup(commandRegistry);
        }
        System.out.println("Hosh v." + Version.readVersion());
        System.out.println("Running on Java " + System.getProperty("java.version"));
        while (true) {
            try {
                String line = lineReader.readLine("hosh> ");
                HoshParser.ProgramContext programContext = Parser.parse(line);
                programContext.stmt().forEach(stmt -> {
                    String commandName = stmt.ID().get(0).getSymbol().getText();
                    Optional<Command> search = commandRegistry.search(commandName);
                    if (search.isPresent()) {
                        search.get().run(new ArrayList<>());
                    } else {
                        System.err.println("command not found");
                    }
                });
            } catch (EndOfFileException e) {
                break;
            } catch (Parser.ParseError e) {
                System.err.println(e.getMessage());
            }
        }
    }


}
