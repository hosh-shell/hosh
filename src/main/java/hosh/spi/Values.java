/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in value objects to be used in @{see Record}.
 * <p>
 * NB: concrete types are not exposed by design.
 */
public class Values {

	private Values() {
	}

	private static final None NONE = new None();

	public static Comparator<Value> noneLast(Comparator<Value> comparator) {
		return new NoneAwareComparator(comparator);
	}

	private static class NoneAwareComparator implements Comparator<Value> {

		private final Comparator<Value> inner;

		private NoneAwareComparator(Comparator<Value> inner) {
			this.inner = inner;
		}

		@Override
		public int compare(Value a, Value b) {
			if (a instanceof None) {
				return (b instanceof None) ? 0 : 1;
			} else if (b instanceof None) {
				return -1;
			} else {
				return inner.compare(a, b);
			}
		}
	}

	public static Value none() {
		return NONE;
	}

	public static Value ofDuration(Duration duration) {
		return new DurationValue(duration);
	}

	public static Value ofInstant(Instant instant) {
		return new InstantValue(instant);
	}

	public static Value ofNumeric(long number) {
		return new NumericValue(number);
	}

	/**
	 * Generic text.
	 */
	public static Value ofText(String text) {
		return new TextValue(text);
	}

	/**
	 * Used to represent a size of a file, etc.
	 */
	public static Value ofSize(long bytes) {
		return new SizeValue(bytes);
	}

	public static Value ofPath(Path path) {
		return new PathValue(path);
	}

	public static Value withStyle(Value value, Ansi.Style style) {
		return new StyledValue(value, style);
	}

	static final class TextValue implements Value {

		private final String value;

		public TextValue(String value) {
			if (value == null) {
				throw new IllegalArgumentException("text cannot be null");
			}
			this.value = value;
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			printWriter.append(value);
		}

		@Override
		public String toString() {
			return String.format("Text[%s]", value);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TextValue) {
				TextValue that = (TextValue) obj;
				return Objects.equals(this.value, that.value);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		private static final Comparator<String> ALPHANUM = new AlphaNumericStringComparator();

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof TextValue) {
				TextValue that = (TextValue) obj;
				return ALPHANUM.compare(this.value, that.value);
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Optional<T> unwrap(Class<T> type) {
			if (type.isAssignableFrom(String.class)) {
				return (Optional<T>) Optional.of(value);
			}
			return Optional.empty();
		}
	}

	static final class SizeValue implements Value {

		private final long bytes;

		public SizeValue(long bytes) {
			if (bytes < 0) {
				throw new IllegalArgumentException("negative size");
			}
			this.bytes = bytes;
		}

		private enum Unit {
			B, KB, MB, GB, TB
		}

		// One kibibyte (1024 bytes), this is in contrast to the SI system (1000 bytes)
		private static final int KIB = 1024;

		// log-indexed units table
		private static final Unit[] UNITS = {Unit.KB, Unit.MB, Unit.GB, Unit.TB};

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			BigDecimal value;
			Unit unit;
			if (bytes < KIB) {
				unit = Unit.B;
				value = BigDecimal.valueOf(bytes);
			} else {
				int exp = (int) (Math.log(bytes) / Math.log(KIB));
				unit = UNITS[exp - 1];
				value = BigDecimal.valueOf(bytes).divide(BigDecimal.valueOf(Math.pow(KIB, exp)), 1, RoundingMode.HALF_UP);
			}
			NumberFormat instance = NumberFormat.getInstance(locale);
			printWriter.append(instance.format(value));
			printWriter.append(unit.toString());
		}

		@Override
		public String toString() {
			return String.format("Size[%sB]", bytes);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SizeValue) {
				SizeValue that = (SizeValue) obj;
				return this.bytes == that.bytes;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Long.hashCode(bytes);
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof SizeValue) {
				SizeValue that = (SizeValue) obj;
				return Long.compare(this.bytes, that.bytes);
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Optional<T> unwrap(Class<T> type) {
			if (type == Long.class) {
				return (Optional<T>) Optional.of(bytes);
			}
			return Optional.empty();
		}
	}

	static final class NumericValue implements Value {

		private final long number;

		public NumericValue(long number) {
			this.number = number;
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			NumberFormat instance = NumberFormat.getInstance(locale);
			printWriter.append(instance.format(number));
		}

		@Override
		public String toString() {
			return String.format("Numeric[%s]", number);
		}

		@Override
		public int hashCode() {
			return Long.hashCode(number);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof NumericValue) {
				NumericValue that = (NumericValue) obj;
				return this.number == that.number;
			} else {
				return false;
			}
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof NumericValue) {
				NumericValue that = (NumericValue) obj;
				return Long.compare(this.number, that.number);
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}
	}

	static final class None implements Value {

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			printWriter.append("");
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
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
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
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			printWriter.append(duration.toString());
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

	static final class InstantValue implements Value {

		private final Instant instant;

		public InstantValue(Instant instant) {
			if (instant == null) {
				throw new IllegalArgumentException("instant cannot be null");
			}
			this.instant = instant;
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof InstantValue) {
				InstantValue that = (InstantValue) obj;
				return this.instant.compareTo(that.instant);
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			printWriter.append(instant.toString());
		}

		@Override
		public String toString() {
			return String.format("Instant[%s]", instant);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof InstantValue) {
				InstantValue that = (InstantValue) obj;
				return Objects.equals(this.instant, that.instant);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(instant);
		}
	}

	static final class PathValue implements Value {

		private final Path path;

		public PathValue(Path path) {
			if (path == null) {
				throw new IllegalArgumentException("path cannot be null");
			}
			this.path = path;
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			printWriter.append(path.toString());
		}

		@Override
		public String toString() {
			return String.format("Path[%s]", path);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PathValue) {
				PathValue that = (PathValue) obj;
				return Objects.equals(this.path, that.path);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(path);
		}

		@Override
		public <T> Optional<T> unwrap(Class<T> type) {
			if (type.equals(Path.class)) {
				return Optional.of(type.cast(path));
			}
			return Optional.empty();
		}

		private static final Comparator<Path> PATH_COMPARATOR =
			Comparator.comparing(Path::toString, new AlphaNumericStringComparator());

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof PathValue) {
				PathValue that = (PathValue) obj;
				return PATH_COMPARATOR.compare(this.path, that.path);
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}
	}

	/**
	 * The Alphanum Algorithm is an improved sorting algorithm for strings
	 * containing numbers. Instead of sorting numbers in ASCII order like
	 * a standard sort, this algorithm sorts numbers in numeric order.
	 * <p>
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

	static final class StyledValue implements Value {

		private final Value value;
		private final Ansi.Style style;

		public StyledValue(Value value, Ansi.Style style) {
			if (value == null) {
				throw new IllegalArgumentException("value cannot be null");
			}
			if (style == null) {
				throw new IllegalArgumentException("style cannot be null");
			}
			this.value = value;
			this.style = style;
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			style.enable(printWriter);
			value.print(printWriter, locale);
			style.disable(printWriter);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof StyledValue) {
				StyledValue that = (StyledValue) obj;
				return Objects.equals(this.value, that.value) && Objects.equals(this.style, that.style);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(value, style);
		}

		@Override
		public String toString() {
			return String.format("StyledValue[value=%s,style='%s']", value, style);
		}

		private static final Comparator<StyledValue> BY_VALUE_AND_STYLE =
			Comparator
				.comparing(StyledValue::value)
				.thenComparing(StyledValue::style);

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof StyledValue) {
				StyledValue that = (StyledValue) obj;
				return BY_VALUE_AND_STYLE.compare(this, that);
			} else {
				throw new IllegalArgumentException("cannot compare " + this + " to " + obj);
			}
		}

		@Override
		public <T> Optional<T> unwrap(Class<T> type) {
			return value.unwrap(type);
		}

		private Value value() {
			return value;
		}

		private Ansi.Style style() {
			return style;
		}
	}
}
