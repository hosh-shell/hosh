package org.hosh.runtime;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class CommandCompleter implements Completer {

	private final State state;

	public CommandCompleter(@Nonnull State state) {
		this.state = state;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		Set<String> keySet = state.getCommands().keySet();
		candidates.addAll(keySet.stream().map(Candidate::new).collect(Collectors.toList()));
	}

}
