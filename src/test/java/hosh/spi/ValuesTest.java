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

import hosh.spi.Values.AlphaNumericStringComparator;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

public class ValuesTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class NoneValueTest {

		@Mock
		private PrintWriter printWriter;

		@Test
		public void append() {
			Values.none().print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.None.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.none()).hasToString("None");
		}

		@Test
		public void unwrap() {
			assertThat(Values.none().unwrap(Object.class)).isEmpty();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DurationValueTest {

		@Mock
		private PrintWriter printWriter;

		@Test
		public void append() {
			Values.ofDuration(Duration.ofMillis(1)).print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("PT0.001S");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.DurationValue.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofDuration(Duration.ofMillis(1))).hasToString("Duration[PT0.001S]");
		}

		@Test
		public void nullDuration() {
			assertThatThrownBy(() -> Values.ofDuration(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("duration cannot be null");
		}

		@Test
		public void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofDuration(Duration.ofHours(1)).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Duration[PT1H] to Text[2]");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class InstantValueTest {

		@Mock
		private PrintWriter printWriter;

		@Test
		public void append() {
			Values.ofInstant(Instant.EPOCH).print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("1970-01-01T00:00:00Z");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.InstantValue.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofInstant(Instant.EPOCH)).hasToString("Instant[1970-01-01T00:00:00Z]");
		}

		@Test
		public void nullDuration() {
			assertThatThrownBy(() -> Values.ofInstant(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("instant cannot be null");
		}

		@Test
		public void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofInstant(Instant.EPOCH).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Instant[1970-01-01T00:00:00Z] to Text[2]");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TextValueTest {

		@Mock
		private PrintWriter printWriter;

		@Test
		public void nullIsReject() {
			assertThatThrownBy(() -> Values.ofText(null))
				.hasMessage("text cannot be null")
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		public void appendOk() {
			Values.ofText("aaa").print(printWriter, Locale.getDefault());
			then(printWriter).should().append("aaa");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.TextValue.class)
				.withIgnoredFields("style")
				.verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofText("aaa")).hasToString("Text[aaa]");
		}

		@Test
		public void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofText("2").compareTo(Values.ofDuration(Duration.ofHours(1))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Text[2] to Duration[PT1H]");
		}

		@Test
		public void nullText() {
			assertThatThrownBy(() -> Values.ofText(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("text cannot be null");
		}

		@Test
		public void nullStyles() {
			assertThatThrownBy(() -> Values.ofStyledText("asd", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("style cannot be null");
		}

		@Test
		public void unwrap() {
			Value value = Values.ofText("aaa");
			assertThat(value.unwrap(int.class)).isEmpty();
			assertThat(value.unwrap(String.class)).hasValue("aaa");
			assertThat(value.unwrap(CharSequence.class)).hasValue("aaa");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class NumericValueTest {

		@Mock
		private PrintWriter printWriter;

		@Test
		public void englishLocale() {
			Values.ofNumeric(1_000_000).print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("1,000,000");
		}

		@Test
		public void italianLocale() {
			Values.ofNumeric(1_000_000).print(printWriter, Locale.ITALIAN);
			then(printWriter).should().append("1.000.000");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.NumericValue.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofNumeric(1)).hasToString("Numeric[1]");
		}

		@Test
		public void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofNumeric(42).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Numeric[42] to Text[2]");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SizeValueTest {

		@Mock
		private PrintWriter printWriter;

		@ParameterizedTest
		@CsvSource({
			"         0,     0,  B",
			"        10,    10,  B",
			"      1023, 1.023,  B",
			"      1024,     1, KB",
			"      2048,     2, KB",
			"      4096,     4, KB",
			"   1048576,     1, MB",
		})
		public void approximateOnPrint(ArgumentsAccessor args) {
			long bytes = args.getLong(0);
			String expectedValue = args.getString(1);
			String expectedUnit = args.getString(2);
			Values.ofSize(bytes).print(printWriter, Locale.ITALIAN);
			then(printWriter).should().append(expectedValue);
			then(printWriter).should().append(expectedUnit);
		}

		@Test
		public void appendWithUkLocale() {
			long bytes = 1024 * 2 + 1024 / 2;
			Values.ofSize(bytes).print(printWriter, Locale.UK);
			then(printWriter).should().append("2.5");
			then(printWriter).should().append("KB");
		}

		@Test
		public void appendWithItalianLocale() {
			long bytes = 1024 * 2 + 1024 / 2;
			Values.ofSize(bytes).print(printWriter, Locale.ITALIAN);
			then(printWriter).should().append("2,5");
			then(printWriter).should().append("KB");
		}

		@Test
		public void asString() {
			assertThat(Values.ofSize(0L)).hasToString("Size[0B]");
			assertThat(Values.ofSize(512L)).hasToString("Size[512B]");
			assertThat(Values.ofSize(1023L)).hasToString("Size[1023B]");
		}

		@Test
		public void size() {
			assertThatThrownBy(() -> Values.ofSize(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("negative size");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.SizeValue.class).verify();
		}

		@Test
		public void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofSize(1000).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Size[1000B] to Text[2]");
		}

		@Test
		public void unwrap() {
			Value value = Values.ofSize(10);
			assertThat(value.unwrap(Long.class)).hasValue(10L);
			assertThat(value.unwrap(Integer.class)).isEmpty();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class PathValueTest {

		@Mock
		private PrintWriter printWriter;

		@Test
		public void appendOk() {
			Values.ofPath(Paths.get(".")).print(printWriter, Locale.getDefault());
			then(printWriter).should().append(".");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.PathValue.class)
				.withIgnoredFields("style")
				.verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofPath(Paths.get("file"))).hasToString("Path[file]");
		}

		@Test
		public void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofPath(Paths.get("file")).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Path[file] to Text[2]");
		}

		@Test
		public void nullPath() {
			assertThatThrownBy(() -> Values.ofPath(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("path cannot be null");
		}

		@Test
		public void nullStyle() {
			assertThatThrownBy(() -> Values.ofStyledPath(Paths.get("file"), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("style cannot be null");
		}

		@Test
		public void unwrap() {
			Value value = Values.ofPath(Paths.get("."));
			assertThat(value.unwrap(Path.class)).isPresent();
			assertThat(value.unwrap(Integer.class)).isEmpty();
		}
	}

	@Nested
	public class AlphaNumericStringComparatorTest {

		@Test
		public void sortLetters() {
			List<String> input = Arrays.asList("b", "c", "a", "ad", "a");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("a", "a", "ad", "b", "c");
		}

		@Test
		public void sortIntegers() {
			List<String> input = Arrays.asList("2", "20", "10", "1");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("1", "2", "10", "20");
		}

		@Test
		public void sortDoubles() {
			List<String> input = Arrays.asList("1.0", "1.3", "1.2", "1.1");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("1.0", "1.1", "1.2", "1.3");
		}

		@Test
		public void sortWithNumberSuffix() {
			List<String> input = Arrays.asList("foo2", "foo20", "foo10", "foo1");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("foo1", "foo2", "foo10", "foo20");
		}

		@Test
		public void sortEmpty() {
			List<String> input = Arrays.asList("", "");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("", "");
		}

		@Test
		public void sortEquals() {
			List<String> input = Arrays.asList("a", "a", "a", "a");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("a", "a", "a", "a");
		}

		@Test
		public void sortDifferentLengths() {
			List<String> input = Arrays.asList("", "a", "a1a", "a1", "a1aaa", "a1aa");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("", "a", "a1", "a1a", "a1aa", "a1aaa");
		}

		@Test
		public void sortDates() {
			List<String> input = Arrays.asList("20180604", "20180603", "20180602", "20180601");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("20180601", "20180602", "20180603", "20180604");
		}

		@Test
		public void sortMisc() {
			List<String> input = Arrays.asList("a.1", "1.a", "2.a", "b.1");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("1.a", "2.a", "a.1", "b.1");
		}
	}

	@Nested
	public class SortingBetweenValuesTest {

		@Test
		public void instantWithNone() {
			List<Value> sorted = Stream.of(
				Values.ofInstant(Instant.ofEpochMilli(100)),
				Values.none(),
				Values.none(),
				Values.ofInstant(Instant.ofEpochMilli(-100)),
				Values.ofInstant(Instant.ofEpochMilli(0)))
				                     .sorted()
				                     .collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.none(),
				Values.none(),
				Values.ofInstant(Instant.ofEpochMilli(-100)),
				Values.ofInstant(Instant.ofEpochMilli(0)),
				Values.ofInstant(Instant.ofEpochMilli(100)));
		}

		@Test
		public void numericWithNone() {
			List<Value> sorted = Stream.of(
				Values.ofNumeric(1),
				Values.none(),
				Values.none(),
				Values.ofNumeric(-1),
				Values.ofNumeric(0))
				                     .sorted()
				                     .collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.none(),
				Values.none(),
				Values.ofNumeric(-1),
				Values.ofNumeric(0),
				Values.ofNumeric(1));
		}

		@Test
		public void textWithNone() {
			List<Value> sorted = Stream.of(
				Values.ofText("a"),
				Values.none(),
				Values.none(),
				Values.ofText("z"),
				Values.ofText("b"))
				                     .sorted()
				                     .collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.none(),
				Values.none(),
				Values.ofText("a"),
				Values.ofText("b"),
				Values.ofText("z"));
		}

		@Test
		public void sizeWithNone() {
			List<Value> sorted = Stream.of(
				Values.ofSize(1),
				Values.none(),
				Values.none(),
				Values.ofSize(2),
				Values.ofSize(3))
				                     .sorted()
				                     .collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.none(),
				Values.none(),
				Values.ofSize(1),
				Values.ofSize(2),
				Values.ofSize(3));
		}

		@Test
		public void pathWithNone() {
			List<Value> sorted = Stream.of(
				Values.ofPath(Paths.get("bbb")),
				Values.none(),
				Values.none(),
				Values.ofPath(Paths.get("aaa")),
				Values.ofPath(Paths.get("ccc")))
				                     .sorted()
				                     .collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.none(),
				Values.none(),
				Values.ofPath(Paths.get("aaa")),
				Values.ofPath(Paths.get("bbb")),
				Values.ofPath(Paths.get("ccc")));
		}

		@Test
		public void durationWithNone() {
			List<Value> sorted = Stream.of(
				Values.none(),
				Values.ofDuration(Duration.ofMillis(1)),
				Values.none(),
				Values.ofDuration(Duration.ofMillis(2)))
				                     .sorted()
				                     .collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.none(),
				Values.none(),
				Values.ofDuration(Duration.ofMillis(1)),
				Values.ofDuration(Duration.ofMillis(2)));
		}
	}
}
