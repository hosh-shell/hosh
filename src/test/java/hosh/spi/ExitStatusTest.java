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
package hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ExitStatusTest {

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(ExitStatus.class).verify();
	}

	@Test
	public void repr() {
		assertThat(ExitStatus.of(42)).hasToString("ExitStatus[value=42]");
	}

	@Test
	public void of() {
		// Tautological test, but it is important to preserve this contract
		assertThat(ExitStatus.of(42).value()).isEqualTo(42);
	}

	@Test
	public void success() {
		assertThat(ExitStatus.success().value()).isEqualTo(0);
	}

	@Test
	public void error() {
		assertThat(ExitStatus.error().value()).isEqualTo(1);
	}

	@Test
	public void parseValid() {
		Optional<ExitStatus> parsed = ExitStatus.parse("42");
		assertThat(parsed)
				.isNotEmpty()
				.hasValue(ExitStatus.of(42));
	}

	@Test
	public void parseInvalidText() {
		Optional<ExitStatus> parsed = ExitStatus.parse("asd");
		assertThat(parsed)
				.isEmpty();
	}
}
