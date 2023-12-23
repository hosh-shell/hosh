/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
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

	/**
	 * Used to represent array of bytes, encoded as hexadecimal and separated by ':' by default.
	 * <p>
	 * NB: it will be used to represent also message digest such as md5 and sha1.
	 */
	public static Value ofBytes(byte[] bytes) {
		return new BytesValue(bytes);
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
			if (obj instanceof TextValue that) {
				return Objects.equals(this.value, that.value);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}

		private static final Comparator<String> BY_TEXT_ALPHA_NUM = new Comparators.NaturalSortOrder();

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof TextValue that) {
				return BY_TEXT_ALPHA_NUM.compare(this.value, that.value);
			} else {
				return cannotCompare(this, obj);
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
			double value;
			Unit unit;
			if (bytes < KIB) {
				unit = Unit.B;
				value = bytes;
			} else {
				int exp = (int) (Math.log(bytes) / Math.log(KIB));
				unit = UNITS[exp - 1];
				value = bytes / Math.pow(KIB, exp);
			}
			var formatter = new DecimalFormat("#.#", new DecimalFormatSymbols(locale));
			formatter.setRoundingMode(RoundingMode.HALF_UP);
			printWriter.append(formatter.format(value));
			printWriter.append(unit.toString());
		}

		@Override
		public String toString() {
			return String.format("Size[%sB]", bytes);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SizeValue that) {
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
			if (obj instanceof SizeValue that) {
				return Long.compare(this.bytes, that.bytes);
			} else {
				return cannotCompare(this, obj);
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

		@Override
		public Optional<Value> merge(Value value) {
			if (value instanceof SizeValue that) {
				return Optional.of(new SizeValue(that.bytes + this.bytes));
			}
			return value.merge(this);
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
			if (obj instanceof NumericValue that) {
				return this.number == that.number;
			} else {
				return false;
			}
		}

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof NumericValue that) {
				return Long.compare(this.number, that.number);
			} else {
				return cannotCompare(this, obj);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Optional<T> unwrap(Class<T> type) {
			if (type == String.class) {
				return (Optional<T>) Optional.of(Long.toString(number));
			}
			if (type == Long.class) {
				return (Optional<T>) Optional.of(number);
			}
			return Optional.empty();
		}

		@Override
		public Optional<Value> merge(Value value) {
			if (value instanceof NumericValue that) {
				return Optional.of(new NumericValue(that.number + this.number));
			}
			return value.merge(this);
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
				return cannotCompare(this, obj);
			}
		}

		@Override
		public Optional<Value> merge(Value value) {
			// artificially translating None to Optional.empty()
			if (value instanceof None) {
				return Optional.empty();
			}
			return Optional.of(value);
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
			if (obj instanceof DurationValue that) {
				return this.duration.compareTo(that.duration);
			} else {
				return cannotCompare(this, obj);
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
			if (obj instanceof DurationValue that) {
				return Objects.equals(this.duration, that.duration);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(duration);
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
			if (obj instanceof InstantValue that) {
				return this.instant.compareTo(that.instant);
			} else {
				return cannotCompare(this, obj);
			}
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			LocalDateTime localDateTime = LocalDateTime.ofInstant(instant.truncatedTo(ChronoUnit.SECONDS), TimeZone.getDefault().toZoneId());
			printWriter.append(localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		}

		@Override
		public String toString() {
			return String.format("Instant[%s]", instant);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof InstantValue that) {
				return Objects.equals(this.instant, that.instant);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(instant);
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
			if (obj instanceof PathValue that) {
				return Objects.equals(this.path, that.path);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(path);
		}

		@Override
		public <T> Optional<T> unwrap(Class<T> type) {
			if (type.equals(Path.class)) {
				return Optional.of(type.cast(path));
			}
			if (type.equals(String.class)) {
				return Optional.of(type.cast(path.toString()));
			}
			return Optional.empty();
		}

		private static final Comparator<Path> BY_PATH_NATURAL_ORDER =
				Comparator.comparing(Path::toString, new Comparators.NaturalSortOrder());

		@Override
		public int compareTo(Value obj) {
			if (obj instanceof PathValue that) {
				return BY_PATH_NATURAL_ORDER.compare(this.path, that.path);
			} else {
				return cannotCompare(this, obj);
			}
		}
	}

	static class BytesValue implements Value {


		private static final HexFormat HEX_FORMAT = HexFormat.ofDelimiter(":").withLowerCase();

		private final byte[] bytes;

		public BytesValue(byte[] bytes) {
			if (bytes == null) {
				throw new IllegalArgumentException("bytes cannot be null");
			}

			this.bytes = bytes;
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			String hex = HEX_FORMAT.formatHex(bytes);
			printWriter.print(hex);
		}

		@Override
		public int compareTo(Value o) {
			if (o instanceof BytesValue that) {
				return Arrays.compare(this.bytes, that.bytes);
			}
			return cannotCompare(this, o);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof BytesValue that) {
				return Arrays.equals(this.bytes, that.bytes);
			} else {
				return false;
			}
		}

		@Override
		public final int hashCode() {
			return Arrays.hashCode(bytes);
		}

		@Override
		public String toString() {
			return String.format("Bytes[%s]", Arrays.toString(bytes));
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
			if (obj instanceof StyledValue that) {
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
			if (obj instanceof StyledValue that) {
				return BY_VALUE_AND_STYLE.compare(this, that);
			} else {
				return cannotCompare(this, obj);
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

	public static class Comparators {

		private Comparators() {
		}

		/**
		 * The natural sort order is an improved sorting algorithm for strings
		 * containing numbers. Instead of sorting numbers in ASCII order like
		 * a standard sort, this algorithm sorts numbers in numeric order.
		 * <p>
		 * See <a href="https://en.wikipedia.org/wiki/Natural_sort_order">wikipedia</a> for more details.
		 */
		public static Comparator<String> naturalSortOrder() {
			return new NaturalSortOrder();
		}

		public static Comparator<Value> noneLast(Comparator<Value> comparator) {
			return new NoneLastComparator(comparator);
		}

		public static Comparator<Value> noneFirst(Comparator<Value> comparator) {
			return new NoneFirstComparator(comparator);
		}

		static class NaturalSortOrder implements Comparator<String> {

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

		static class NoneLastComparator implements Comparator<Value> {

			private final Comparator<Value> inner;

			private NoneLastComparator(Comparator<Value> inner) {
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

		static class NoneFirstComparator implements Comparator<Value> {

			private final Comparator<Value> inner;

			private NoneFirstComparator(Comparator<Value> inner) {
				this.inner = inner;
			}

			@Override
			public int compare(Value a, Value b) {
				if (a instanceof None) {
					return (b instanceof None) ? 0 : -1;
				} else if (b instanceof None) {
					return 1;
				} else {
					return inner.compare(a, b);
				}
			}
		}

	}

	// generic error for compareTo, when types are not compatible
	private static int cannotCompare(Value a, Value b) {
		throw new IllegalArgumentException("cannot compare " + a + " with " + b);
	}
}
