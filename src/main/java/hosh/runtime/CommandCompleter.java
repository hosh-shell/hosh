/**
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import hosh.spi.LoggerFactory;
import hosh.spi.State;

public class CommandCompleter implements Completer {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private final State state;

	public CommandCompleter(State state) {
		this.state = state;
	}

	@Override
	public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
		Set<String> builtinOverrides = new HashSet<>();
		completeExternals(candidates, builtinOverrides);
		completeBuiltinsExcludingOverrides(candidates, builtinOverrides);
	}

	private void completeBuiltinsExcludingOverrides(List<Candidate> candidates, Set<String> builtinOverrides) {
		state.getCommands().keySet()
				.stream()
				.filter(command -> !builtinOverrides.contains(command))
				.map(command -> Candidates.completeWithDescription(command, "built-in"))
				.forEach(candidates::add);
	}

	private void completeExternals(List<Candidate> candidates, Set<String> builtinOverrides) {
		for (Path path : state.getPath()) {
			if (Files.isDirectory(path)) {
				executableInPath(path, candidates, builtinOverrides);
			}
		}
	}

	private void executableInPath(Path dir, List<Candidate> candidates, Set<String> builtinOverrides) {
		try (Stream<Path> list = Files.list(dir)) {
			list
					.filter(p -> Files.isExecutable(p))
					.map(p -> toCandidate(p, builtinOverrides))
					.forEach(candidates::add);
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "got exception while listing " + dir, e);
		}
	}

	private Candidate toCandidate(Path p, Set<String> builtinOverrides) {
		String name = p.getFileName().toString();
		String description;
		if (state.getCommands().containsKey(name)) {
			description = "built-in, overrides " + p.toAbsolutePath();
			builtinOverrides.add(name);
		} else {
			description = "external in " + p.getParent();
		}
		return Candidates.completeWithDescription(name, description);
	}
}
