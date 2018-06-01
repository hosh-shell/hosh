package org.hosh.runtime;

import org.hosh.spi.CommandRegistry;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.stream.Collectors;

public class CommandCompleter implements Completer {

    private final CommandRegistry commandRegistry;

    public CommandCompleter(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        candidates.addAll(commandRegistry.commandNames().stream().map(Candidate::new).collect(Collectors.toList()));
    }

}
