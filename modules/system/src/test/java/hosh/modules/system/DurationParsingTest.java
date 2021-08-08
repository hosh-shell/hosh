/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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
package hosh.modules.system;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DurationParsingTest {

	@Test
	void iso8601FormatUppercase() {
		Optional<Duration> result = DurationParsing.parse("PT5S");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void iso8601FormatLowercase() {
		Optional<Duration> result = DurationParsing.parse("PT5s");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void ourCustomFormatLowercase() {
		Optional<Duration> result = DurationParsing.parse("5s");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void ourCustomFormatUppercase() {
		Optional<Duration> result = DurationParsing.parse("5S");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void nullValue() {
		Optional<Duration> result = DurationParsing.parse(null);
		assertThat(result).isEmpty();
	}

	@Test
	void emptyValue() {
		Optional<Duration> result = DurationParsing.parse("");
		assertThat(result).isEmpty();
	}

	@Test
	void missingUnit() {
		Optional<Duration> result = DurationParsing.parse("5");
		assertThat(result).isEmpty();
	}
}