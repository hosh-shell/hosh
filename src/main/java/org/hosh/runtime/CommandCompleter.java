package org.hosh.runtime;

import java.util.List;

import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

public class CommandCompleter implements Completer {

	private final State state;

	public CommandCompleter(State state) {
		this.state = state;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		state.getCommands().keySet()
			.stream()
			.map(DebuggableCandidate::new)
			.forEach(c -> candidates.add(c));
	}

}
