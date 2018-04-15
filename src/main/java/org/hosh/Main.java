package org.hosh;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.terminal();
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        System.out.println("Hosh v." + Version.readVersion());
        System.out.println("Running on Java " + System.getProperty("java.version"));
        while (true) {
            try {
                String line = lineReader.readLine("hosh> ");
                Parser.parse(line);
                System.out.println("  ... running");
            } catch (EndOfFileException e) {
                break;
            } catch (Parser.ParseError e) {
                System.err.println(e.getMessage());
            }
        }

    }
}
