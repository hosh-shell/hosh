/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
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
package hosh.spi;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Stream;

/**
 * A read-only sequence of {@link CommandArgument} passed to {@link Command#run}.
 * Exposes exactly what commands need — no mutation, no raw list leakage.
 */
public record CommandArguments(List<CommandArgument> args) implements Iterable<CommandArguments.CommandArgument> {

	public CommandArguments {
		args = List.copyOf(args);
	}

	public static CommandArguments of(String... values) {
		return new CommandArguments(Arrays.stream(values).map(CommandArgument::new).toList());
	}

	public static CommandArguments of(List<CommandArgument> args) {
		return new CommandArguments(List.copyOf(args));
	}

	public boolean isEmpty() {
		return args.isEmpty();
	}

	public int size() {
		return args.size();
	}

	// access the argument
	// it is programming error to not have the requested index
	public CommandArgument get(int index) {
		return args.get(index);
	}

	public Stream<CommandArgument> stream() {
		return args.stream();
	}

	@Override
	public Iterator<CommandArgument> iterator() {
		return args.iterator();
	}

	/**
	 * A single command-line argument with typed accessors.
	 * Internally a plain string — all conversions are explicit and safe.
	 */
	public record CommandArgument(String value) {

		public static CommandArgument of(String value) {
			return new CommandArgument(value);
		}

		public String asString() {
			return value;
		}

		public Key asKey() {
			return Keys.of(value);
		}

		public OptionalLong asLong() {
			try {
				return OptionalLong.of(Long.parseLong(value));
			} catch (NumberFormatException e) {
				return OptionalLong.empty();
			}
		}

		public OptionalInt asInt() {
			try {
				return OptionalInt.of(Integer.parseInt(value));
			} catch (NumberFormatException e) {
				return OptionalInt.empty();
			}
		}

		public Optional<Duration> asDuration() {
			try {
				if (value.startsWith("PT")) {
					return Optional.of(Duration.parse(value));
				}
				return Optional.of(Duration.parse("PT" + value));
			} catch (DateTimeParseException e) {
				return Optional.empty();
			}
		}

		/**
		 * Resolves the current as path relative to the current cwd.
		 */
		public Path asPath(State state) {
			Path path = Path.of(this.asString());
			return resolveAsAbsolutePath(state.getCwd(), path);
		}

		private static Path resolveAsAbsolutePath(Path cwd, Path file) {
			if (file.isAbsolute()) {
				return normalized(file);
			}
			return normalized(cwd.resolve(file));
		}

		private static Path normalized(Path path) {
			return path.normalize().toAbsolutePath();
		}
	}
}
