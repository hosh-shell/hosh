package org.hosh.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
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
			listCandidates(parent(path), Path::toAbsolutePath, candidates);
		} else {
			listCandidates(state.getCwd(), Path::getFileName, candidates);
		}
	}

	// get parent dir, handling special case / (where parent("/") == null)
	private Path parent(Path path) {
		return path.getParent() == null ? path : path.getParent();
	}

	private void listCandidates(Path dir, Function<Path, Path> toCandidate, List<Candidate> candidates) throws IOException {
		logger.info("list '{}'", dir);
		try (Stream<Path> list = Files.list(dir)) {
			list
					.peek(p -> logger.info("  {}", p))
					.map(toCandidate)
					.map(Objects::toString)
					.map(DebuggableCandidate::new)
					.forEach(candidates::add);
		}
	}
}
