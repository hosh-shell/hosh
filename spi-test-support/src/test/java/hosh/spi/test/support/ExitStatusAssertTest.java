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
package hosh.spi.test.support;

import hosh.spi.ExitStatus;
import org.junit.jupiter.api.Test;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;

@SuppressWarnings("MaskedAssertion")
class ExitStatusAssertTest {

	@Test
	void error() {
		// Given
		ExitStatus sut = ExitStatus.of(1);

		// When / Then
		assertThat(sut).isError();
		assertThat(sut).hasExitCode(1);
		try {
			assertThat(sut).isSuccess();
		} catch (AssertionError e) {
			// all good
		}
	}

	@Test
	void success() {
		// Given
		ExitStatus sut = ExitStatus.of(0);

		// When / Then
		assertThat(sut).isSuccess();
		assertThat(sut).hasExitCode(0);
		try {
			assertThat(sut).isError();
		} catch (AssertionError e) {
			// all good
		}
	}

}
