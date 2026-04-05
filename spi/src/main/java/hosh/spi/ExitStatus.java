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

import java.util.Objects;
import java.util.Optional;

/**
 * Value object for describing exit status.
 * <p>
 * It can be used for both from built-in and external commands.
 */
public final class ExitStatus {

	private final int value;

	private ExitStatus(int value) {
		this.value = value;
	}

	/** Returns the canonical success status (exit code 0). */
	public static ExitStatus success() {
		return new ExitStatus(0);
	}

	/** Returns the canonical error status (exit code 1). */
	public static ExitStatus error() {
		return new ExitStatus(1);
	}

	/** Returns an exit status with the given numeric exit code. */
	public static ExitStatus of(int value) {
		return new ExitStatus(value);
	}

	/** Parses an exit status from a decimal string, returning empty if the string is not a valid integer. */
	public static Optional<ExitStatus> parse(String str) {
		try {
			int value = Integer.parseInt(str, 10);
			return Optional.of(new ExitStatus(value));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	/** Returns {@code true} if the exit code is 0. */
	public boolean isSuccess() {
		return value == 0;
	}

	/** Returns {@code true} if the exit code is non-zero. */
	public boolean isError() {
		return value != 0;
	}

	/** Returns the raw numeric exit code. */
	public int value() {
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExitStatus that) {
			return this.value == that.value;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}

	@Override
	public String toString() {
		return String.format("ExitStatus[value=%s]", value);
	}
}
