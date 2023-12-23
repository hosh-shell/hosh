/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Value object representing a variable name in the shell.
 */
public final class VariableName {

	static final int MAX_VARIABLE_LENGTH = 256;

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	// please keep in sync with HoshParser.g4 and HoshLexer.g4
	private static final Pattern VARIABLE = Pattern.compile("[A-Za-z_]+");

	private final String name;

	private VariableName(String name) {
		if (name == null) {
			throw new IllegalArgumentException("variable name must be not null");
		}
		if (name.isBlank()) {
			throw new IllegalArgumentException("variable name must be blank");
		}
		if (name.length() > MAX_VARIABLE_LENGTH) { // arbitrary short name, just to avoid names 2 GB long...
			throw new IllegalArgumentException("variable name too long");
		}
		if (!VARIABLE.matcher(name).matches()) {
			throw new IllegalArgumentException("variable name is invalid");
		}
		this.name = name;
	}

	/**
	 * Named constructor that throws if name is invalid. To be used programmatically (e.g. well-known variables names, tests, etc.)
	 *
	 * @param name the name of the variable
	 * @return the new variable name
	 */
	public static VariableName constant(String name) {
		return new VariableName(name);
	}

	/**
	 * Attempt to build a new variable name from user input
	 *
	 * @param userInput the user input
	 * @return the variable name
	 */
	public static Optional<VariableName> from(String userInput) {
		try {
			return Optional.of(new VariableName(userInput));
		} catch (Exception e) {
			LOGGER.log(Level.INFO, e, () -> String.format("invalid variable name '%s'", userInput));
			return Optional.empty();
		}
	}

	public String name() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof VariableName that) {
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
		return String.format("VariableName[%s]", name);
	}

}
