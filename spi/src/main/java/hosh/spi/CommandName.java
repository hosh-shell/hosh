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

import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing a command name in the shell.
 * <p>
 * Valid command names follow the ID token rule defined in HoshLexer.g4:
 * one or more characters from: letters, digits, ':', '_', '-', '.', '/', '\', '~', '+', '*', '=', '?', '<', '>', '(', ')', ','
 * <p>
 * please keep in sync with HoshParser.g4 and HoshLexer.g4
 */
public final class CommandName {

	// mirrors fragment I in HoshLexer.g4
	private static final java.util.regex.Pattern COMMAND = java.util.regex.Pattern.compile("[A-Za-z0-9:_./\\\\~+*=?<>(),-]+");

	private final String name;

	private CommandName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("command name must be not null");
		}
		if (name.isBlank()) {
			throw new IllegalArgumentException("command name must not be blank");
		}
		if (!COMMAND.matcher(name).matches()) {
			throw new IllegalArgumentException("command name is invalid: " + name);
		}
		this.name = name;
	}

	/**
	 * Named constructor that throws if name is invalid. To be used programmatically (e.g. well-known command names).
	 *
	 * @param name the command name
	 * @return the new command name
	 */
	public static CommandName constant(String name) {
		return new CommandName(name);
	}

	/**
	 * Attempt to build a new command name from user input.
	 *
	 * @param userInput the user input
	 * @return the command name, or empty if invalid
	 */
	public static Optional<CommandName> from(String userInput) {
		try {
			return Optional.of(new CommandName(userInput));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	public String name() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CommandName that) {
			return Objects.equals(this.name, that.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	@Override
	public String toString() {
		return String.format("CommandName[%s]", name);
	}
}
