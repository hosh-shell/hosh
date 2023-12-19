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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

class ExitStatusTest {

	@Test
	void equalsContract() {
		EqualsVerifier.forClass(ExitStatus.class).verify();
	}

	@Test
	void asString() {
		assertThat(ExitStatus.of(42)).hasToString("ExitStatus[value=42]");
	}

	@Test
	void success() {
		ExitStatus exitStatus = ExitStatus.success();
		assertThat(exitStatus.value()).isEqualTo(0);
		assertThat(exitStatus.isSuccess()).isTrue();
		assertThat(exitStatus.isError()).isFalse();
	}

	@Test
	void error() {
		ExitStatus exitStatus = ExitStatus.error();
		assertThat(exitStatus.value()).isEqualTo(1);
		assertThat(exitStatus.isSuccess()).isFalse();
		assertThat(exitStatus.isError()).isTrue();
	}

	@Test
	void validLiterals() {
		qt()
			.forAll(strings().numeric())
			.check(value -> {
				Optional<ExitStatus> parsed = ExitStatus.parse(String.valueOf(value));
				return parsed.isPresent();
			});
	}

	@Test
	void invalidLiteral() {
		qt()
			.forAll(strings().basicLatinAlphabet().ofLengthBetween(0, 10))
			.assuming(value -> value.matches(".*[a-zA-Z].*"))
			.check(value -> {
				Optional<ExitStatus> parsed = ExitStatus.parse(value);
				return parsed.isEmpty();
			});
	}
}
