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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CommandArgumentsTest {

	@Nested
	class DurationArgument {

		@Test
		void iso8601FormatUppercase() {
			// Given
			CommandArguments sut = CommandArguments.of("PT5S");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void iso8601FormatLowercase() {
			// Given
			CommandArguments sut = CommandArguments.of("PT5s");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void ourCustomFormatLowercase() {
			// Given
			CommandArguments sut = CommandArguments.of("5s");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void ourCustomFormatUppercase() {
			// Given
			CommandArguments sut = CommandArguments.of("5S");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void emptyValue() {
			// Given
			CommandArguments sut = CommandArguments.of("");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).isEmpty();
		}

		@Test
		void missingUnit() {
			// Given
			CommandArguments sut = CommandArguments.of("5");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).isEmpty();
		}
	}
}
