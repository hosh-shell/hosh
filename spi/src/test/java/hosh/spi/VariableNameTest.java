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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class VariableNameTest {

	@Test
	void equalsContract() {
		EqualsVerifier.forClass(VariableName.class).verify();
	}

	@Test
	void asString() {
		assertThat(VariableName.constant("FOO")).hasToString("VariableName[FOO]");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"A",
			"Z",
			"a",
			"z",
			"_",
			"__FOO__",
			"VARIABLE",
			"variable",
			"__FOO_BAR__"
	})
	void validVariableNames(String userInput) {
		Optional<VariableName> from = VariableName.from(userInput);
		VariableName of = VariableName.constant(userInput);
		assertThat(from).hasValue(of);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {
			"-",
			"/",
			".",
			"..",
			"  ",
			"",
			"a.",
			"z/",
			"%45",
	})
	void invalidVariableNames(String userInput) {
		Optional<VariableName> from = VariableName.from(userInput);
		assertThat(from).isEmpty();
		assertThatThrownBy(() -> VariableName.constant(userInput))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void veryLongVariableName() {
		String userInput = "a".repeat(257);
		assertThatThrownBy(() -> VariableName.constant(userInput))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("variable name too long");
	}
}