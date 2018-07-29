package org.hosh.spi;

import java.util.Objects;

import org.hosh.doc.Todo;

/**
 * Value object for describing exit status from external as well as internal
 * commands.
 */
@Todo(description = "this should a Value?")
public class ExitStatus {
	private final int value;

	@Todo(description = "check the range of possible exit status on windows")
	private ExitStatus(int value) {
		if (value < 0 || value > 255) {
			throw new IllegalArgumentException("illegal exit status: " + value);
		}
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
