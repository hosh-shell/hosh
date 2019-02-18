/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
package org.hosh.spi;

import java.util.Objects;
import java.util.Optional;

/**
 * Value object for describing exit status from external as well as internal
 * commands.
 */
public class ExitStatus {
	private final int value;

	private ExitStatus(int value) {
		this.value = value;
	}

	public static ExitStatus success() {
		return new ExitStatus(0);
	}

	public static ExitStatus error() {
		return new ExitStatus(1);
	}

	public static ExitStatus of(int value) {
		return new ExitStatus(value);
	}

	public static Optional<ExitStatus> parse(String str) {
		try {
			int value = Integer.parseInt(str, 10);
			return Optional.of(new ExitStatus(value));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public boolean isSuccess() {
		return value == 0;
	}

	public boolean isError() {
		return !isSuccess();
	}

	public int value() {
		return value;
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof ExitStatus) {
			ExitStatus that = (ExitStatus) obj;
			return this.value == that.value;
		} else {
			return false;
		}
	}

	@Override
	public final int hashCode() {
		return Objects.hash(value);
	}

	@Override
	public String toString() {
		return String.format("ExitStatus[value=%s]", value);
	}
}
