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
		if (value < 0 || value > 255) {
			throw new InvalidExitCode("illegal exit status: " + value);
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

	/**
	 * Malformed (e.g. 'not a number') or invalid exit values (e.g. -1)
	 * yield no value.
	 */
	public static Optional<ExitStatus> parse(String str) {
		try {
			int value = Integer.parseInt(str, 10);
			return Optional.of(new ExitStatus(value));
		} catch (NumberFormatException | InvalidExitCode e) {
			return Optional.empty();
		}
	}

	public boolean isSuccess() {
		return value == 0;
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

	public static class InvalidExitCode extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public InvalidExitCode(String message) {
			super(message);
		}
	}
}
