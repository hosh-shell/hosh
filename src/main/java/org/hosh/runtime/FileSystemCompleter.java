package org.hosh.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemCompleter implements Completer {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final State state;

	public FileSystemCompleter(State state) {
		this.state = state;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		logger.info("current '{}', candidates {}", line.word(), candidates);
		Path path = Paths.get(line.word());
		if (!path.isAbsolute()) {
			path = state.getCwd();
		}
		populate(candidates, path);
	}

	private void populate(List<Candidate> candidates, Path path) {
		try {
			Files.list(path)
					.map(p -> new DebuggableCandidate(p.getFileName().toString()))
					.peek(p -> logger.info("  {}", p))
					.forEach(c -> candidates.add(c));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
