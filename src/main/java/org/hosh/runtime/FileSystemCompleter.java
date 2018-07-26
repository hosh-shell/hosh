package org.hosh.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

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
		try {
			tryComplete(line, candidates);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	private void tryComplete(ParsedLine line, List<Candidate> candidates) throws IOException {
		Path path = Paths.get(line.word());
		if (path.isAbsolute()) {
			list(path.getParent() == null ? path : path.getParent(), (p) -> p.toString(), candidates);
		} else {
			list(state.getCwd(), (p) -> p.getFileName().toString(), candidates);

		}
	}

	private void list(Path dir, Function<Path, String> toCandidate, List<Candidate> candidates) throws IOException {
		logger.info("list '{}'", dir);
		try (Stream<Path> list = Files.list(dir)) {
			list
					.peek(p -> logger.info("  {}", p))
					.map(toCandidate)
					.map(p -> new DebuggableCandidate(p))
					.forEach(c -> candidates.add(c));
		}
	}
}
