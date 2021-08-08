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

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Internal helper, could be promoted later to be part of hosh-spi and Values.ofDuration.
 */
public class DurationParsing {

	/**
	 * Parsing duration ISO 8601 format with possibility to omit leading 'PT' prefix.
	 *
	 * Any invalid input, including null, returns empty.
	 */
	public static Optional<Duration> parse(String value) {
		try {
			Duration parsed;
			if (value == null) {
				parsed = null;
			} else if (value.startsWith("PT")) {
				parsed = Duration.parse(value);
			} else {
				parsed = Duration.parse("PT" + value);
			}
			return Optional.ofNullable(parsed);
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	private DurationParsing() {
		// to keep sonar happy :-)
	}

}
