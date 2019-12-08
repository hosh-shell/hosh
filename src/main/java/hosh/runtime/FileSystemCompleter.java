/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.runtime;

import hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class FileSystemCompleter implements Completer {

	private final State state;

	public FileSystemCompleter(State state) {
		this.state = state;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		Path path = Paths.get(line.word());
		if (line.word().endsWith(File.separator)) {
			listCandidates(path, p -> p, candidates);
		} else if (path.isAbsolute()) {
			listCandidates(parent(path), p -> p.toAbsolutePath(), candidates);
		} else if (path.getParent() != null) {
			Path resolved = state.getCwd().resolve(path.getParent());
			int from = state.getCwd().getNameCount();
			int to = resolved.getNameCount() + 1;
			listCandidates(resolved, p -> p.subpath(from, to), candidates);
		} else {
			listCandidates(state.getCwd(), p -> p.getFileName(), candidates);
		}
	}

	// get parent dir, handling special case / (where parent("/") == null)
	private Path parent(Path path) {
		return path.getParent() == null ? path : path.getParent();
	}

	private void listCandidates(Path dir, UnaryOperator<Path> toPath, List<Candidate> candidates) {
		try (Stream<Path> list = Files.list(dir)) {
			list
				.map(toPath)
				.map(this::toCandidate)
				.forEach(candidates::add);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Candidate toCandidate(Path path) {
		Path resolved;
		if (path.isAbsolute()) {
			resolved = path;
		} else {
			resolved = state.getCwd().resolve(path);
		}
		if (Files.isDirectory(resolved)) {
			return Candidates.incomplete(path.toString() + File.separator);
		} else {
			return Candidates.complete(path.toString());
		}
	}
}
