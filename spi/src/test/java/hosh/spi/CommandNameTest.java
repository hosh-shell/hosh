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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class CommandNameTest {

	@Test
	void equalsContract() {
		// Given
		// (no setup)

		// When / Then
		EqualsVerifier.forClass(CommandName.class).verify();
	}

	@Test
	void asString() {
		// Given
		// (no setup)

		// When / Then
		assertThat(CommandName.constant("ls")).hasToString("CommandName[ls]");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"ls",
			"cd",
			"withTimeout",
			"withLock",
			"cp",
			"mv",
			"rm",
			"git-commit",
			"some.command",
			"cmd123",
			"*",
			"?",
	})
	void validCommandNames(String userInput) {
		// When
		Optional<CommandName> result = CommandName.from(userInput);

		// Then
		assertThat(result).map(CommandName::name).hasValue(userInput);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
			"",
			"  ",
			"%45",
			"cmd name",
	})
	void invalidCommandNames(String userInput) {
		// When
		Optional<CommandName> result = CommandName.from(userInput);

		// Then
		assertThat(result).isEmpty();
		assertThatThrownBy(() -> CommandName.constant(userInput))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
