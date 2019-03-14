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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in values to be used in Records.
 *
 * NB: concrete types are not exposed by design
 */
public class Values {
	private Values() {
	}

	private static final None NONE = new None();

	public static Value none() {
		return NONE;
	}

	public static Value ofDuration(Duration duration) {
		return new DurationValue(duration);
	}

	public static Value ofNumeric(long number) {
		return new Numeric(number);
	}

	public static Value ofText(String text) {
		return new Text(text);
	}

	public enum Unit {
		B, KB, MB, GB, TB
	}

	/**
	 * One kibibyte (1024 bytes), this is in contrast to the SI system (1000 bytes)
	 */
	public static final int KIB = 1024;
	// log-indexed units table
	private static final Unit[] UNITS = { Unit.KB, Unit.MB, Unit.GB, Unit.TB };

	/**
	 * Select the appropriate unit for measuring bytes.
	 */
	public static Value ofHumanizedSize(long bytes) {
		if (bytes < KIB) {
			return new Size(BigDecimal.valueOf(bytes), Unit.B);
		}
		int exp = (int) (Math.log(bytes) / Math.log(KIB));
		Unit unit = UNITS[exp - 1];
		BigDecimal value = BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(Math.pow(KIB, exp)), 1, RoundingMode.HALF_UP);
		return new Size(value, unit);
	}

	/**
	 * Paths, without any special attributes.
	 */
	public static Value ofLocalPath(Path path) {
		return new LocalPath(path);
	}

	/**
	 * Generic text, make sure that text hasn't any number or date formatted without
	 * the current locale.
	 */
	static final class Text implements Value {
		private final String value;

		public Text(String value) {
			if (value == null) {
				throw new IllegalArgumentException("text cannot be null");
			}
			this.value = value;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append(value);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Text[%s]", value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Text) {
				Text that = (Text) obj;
				return Objects.equals(this.value, that.value);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof Text) {
				Text that = (Text) obj;
				return this.value.compareTo(that.value);
			} else if (obj instanceof None) {
				return 1;
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}

		@Override
		public boolean matches(Object obj) {
			if (obj instanceof String) {
				return value.matches((String) obj);
			} else {
				return false;
			}
		}
	}

	/**
	 * Used to represent a size of a file, etc.
	 */
	static final class Size implements Value {
		private final BigDecimal value;
		private final Unit unit;

		public Size(BigDecimal value, Unit unit) {
			if (value.compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("negative size");
			}
			this.value = value;
			this.unit = unit;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			NumberFormat instance = NumberFormat.getInstance(locale);
			try {
				appendable.append(instance.format(value));
				appendable.append(unit.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Size[%s%s]", value, unit);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Size) {
				Size that = (Size) obj;
				return Objects.equals(this.value, that.value) && Objects.equals(this.unit, that.unit);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, unit);
		}

		private static final Comparator<Size> SIZE_COMPARATOR = Comparator
				.comparing(Size::getUnit)
				.thenComparing(Size::getValue);

		public BigDecimal getValue() {
			return value;
		}

		public Unit getUnit() {
			return unit;
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof Size) {
				Size that = (Size) obj;
				return SIZE_COMPARATOR.compare(this, that);
			} else if (obj instanceof None) {
				return 1;
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}
	}

	static final class Numeric implements Value {
		private final long number;

		public Numeric(long number) {
			this.number = number;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			NumberFormat instance = NumberFormat.getInstance(locale);
			try {
				appendable.append(instance.format(number));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Numeric[%s]", number);
		}

		@Override
		public int hashCode() {
			return Objects.hash(number);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Numeric) {
				Numeric that = (Numeric) obj;
				return this.number == that.number;
			} else {
				return false;
			}
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof Numeric) {
				Numeric that = (Numeric) obj;
				return Long.compare(this.number, that.number);
			} else if (obj instanceof None) {
				return 1;
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}
	}

	static final class None implements Value {
		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append("");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return "None";
		}

		@Override
		public int hashCode() {
			return 17;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof None;
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof None) {
				return 0;
			} else {
				return -1;
			}
		}
	}

	static final class DurationValue implements Value {
		private final Duration duration;

		public DurationValue(Duration duration) {
			if (duration == null) {
				throw new IllegalArgumentException("duration cannot be null");
			}
			this.duration = duration;
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof DurationValue) {
				DurationValue that = (DurationValue) obj;
				return this.duration.compareTo(that.duration);
			} else if (obj instanceof None) {
				return 1;
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append(duration.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("Duration[%s]", duration);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof DurationValue) {
				DurationValue that = (DurationValue) obj;
				return Objects.equals(this.duration, that.duration);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(duration);
		}
	}

	static final class LocalPath implements Value {
		private final Path path;

		public LocalPath(Path path) {
			if (path == null) {
				throw new IllegalArgumentException("path cannot be null");
			}
			this.path = path;
		}

		@Override
		public void append(Appendable appendable, Locale locale) {
			try {
				appendable.append(path.toString());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public String toString() {
			return String.format("LocalPath[%s]", path);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LocalPath) {
				LocalPath that = (LocalPath) obj;
				return Objects.equals(this.path, that.path);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(path);
		}

		private static final Comparator<Path> PATH_COMPARATOR = Comparator
				.comparing(Path::toString, new AlphaNumericStringComparator());

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof LocalPath) {
				LocalPath that = (LocalPath) obj;
				return PATH_COMPARATOR.compare(this.path, that.path);
			} else if (obj instanceof None) {
				return 1;
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}
	}

	/**
	 * The Alphanum Algorithm is an improved sorting algorithm for strings
	 * containing numbers. Instead of sorting numbers in ASCII order like
	 * a standard sort, this algorithm sorts numbers in numeric order.
	 *
	 * The Alphanum Algorithm is discussed at http://www.DaveKoelle.com
	 */
	static class AlphaNumericStringComparator implements Comparator<String> {
		private static final Pattern CHUNK = Pattern.compile("(\\d+)|(\\D+)");

		@Override
		public int compare(String s1, String s2) {
			int compareValue = 0;
			Matcher s1ChunkMatcher = CHUNK.matcher(s1);
			Matcher s2ChunkMatcher = CHUNK.matcher(s2);
			while (s1ChunkMatcher.find() && s2ChunkMatcher.find() && compareValue == 0) {
				String s1ChunkValue = s1ChunkMatcher.group();
				String s2ChunkValue = s2ChunkMatcher.group();
				try {
					Integer s1Integer = Integer.valueOf(s1ChunkValue);
					Integer s2Integer = Integer.valueOf(s2ChunkValue);
					compareValue = s1Integer.compareTo(s2Integer);
				} catch (NumberFormatException e) {
					// not a number, use string comparison.
					compareValue = s1ChunkValue.compareTo(s2ChunkValue);
				}
				// if they are equal thus far, but one has more left, it should come after the
				// one that doesn't.
				if (compareValue == 0) {
					if (s1ChunkMatcher.hitEnd() && !s2ChunkMatcher.hitEnd()) {
						compareValue = -1;
					} else if (!s1ChunkMatcher.hitEnd() && s2ChunkMatcher.hitEnd()) {
						compareValue = 1;
					}
				}
			}
			return compareValue;
		}
	}
}
