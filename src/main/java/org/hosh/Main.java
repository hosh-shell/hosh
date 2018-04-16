package org.hosh;

import org.hosh.antlr4.HoshParser;
import org.hosh.modules.TerminalModule;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class Main {

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.terminal();
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        CommandRegistry commandRegistry = new SimpleCommandRegistry();
        new TerminalModule().beforeStart(commandRegistry);
        System.out.println("Hosh v." + Version.readVersion());
        System.out.println("Running on Java " + System.getProperty("java.version"));
        while (true) {
            try {
                String line = lineReader.readLine("hosh> ");
                HoshParser.ProgramContext programContext = Parser.parse(line);
                programContext.stmt().forEach(stmt -> {
                    String commandName = stmt.ID(0).getSymbol().getText();
                    Optional<Command> search = commandRegistry.search(commandName);
                    if (search.isPresent()) {
                        search.get().run(terminal, new ArrayList<>());
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
