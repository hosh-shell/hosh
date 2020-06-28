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

import hosh.test.support.WithTimeZone;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

class ValuesTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class NoneValueTest {

		@Mock
		PrintWriter printWriter;

		@Test
		void append() {
			Values.none().print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.None.class).verify();
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.none().compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare None to Text[2]");
		}

		@Test
		void compareToNone() {
			Value v1 = Values.none();
			Value v2 = Values.none();
			assertThat(v1).usingDefaultComparator().isEqualByComparingTo(v2);
		}

		@Test
		void asString() {
			assertThat(Values.none()).hasToString("None");
		}

		@Test
		void unwrap() {
			assertThat(Values.none().unwrap(Object.class)).isEmpty();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class DurationValueTest {

		@Mock
		PrintWriter printWriter;

		@Test
		void append() {
			Values.ofDuration(Duration.ofMillis(1)).print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("PT0.001S");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.DurationValue.class).verify();
		}

		@Test
		void asString() {
			assertThat(Values.ofDuration(Duration.ofMillis(1))).hasToString("Duration[PT0.001S]");
		}

		@Test
		void nullDuration() {
			assertThatThrownBy(() -> Values.ofDuration(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("duration cannot be null");
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofDuration(Duration.ofHours(1)).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Duration[PT1H] to Text[2]");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class InstantValueTest {

		@RegisterExtension
		WithTimeZone withTimeZone = new WithTimeZone();

		@Mock
		PrintWriter printWriter;

		@Test
		void append() {
			withTimeZone.changeTo(TimeZone.getTimeZone("Europe/Zurich"));
			Values.ofInstant(Instant.EPOCH).print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("1970-01-01T01:00:00");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.InstantValue.class).verify();
		}

		@Test
		void asString() {
			assertThat(Values.ofInstant(Instant.EPOCH)).hasToString("Instant[1970-01-01T00:00:00Z]");
		}

		@Test
		void nullDuration() {
			assertThatThrownBy(() -> Values.ofInstant(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("instant cannot be null");
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofInstant(Instant.EPOCH).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Instant[1970-01-01T00:00:00Z] to Text[2]");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class TextValueTest {

		@Mock
		PrintWriter printWriter;

		@Test
		void nullIsReject() {
			assertThatThrownBy(() -> Values.ofText(null))
				.hasMessage("text cannot be null")
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void appendOk() {
			Values.ofText("aaa").print(printWriter, Locale.getDefault());
			then(printWriter).should().append("aaa");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.TextValue.class)
				.verify();
		}

		@Test
		void asString() {
			assertThat(Values.ofText("aaa")).hasToString("Text[aaa]");
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofText("2").compareTo(Values.ofDuration(Duration.ofHours(1))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Text[2] to Duration[PT1H]");
		}

		@Test
		void nullText() {
			assertThatThrownBy(() -> Values.ofText(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("text cannot be null");
		}

		@Test
		void unwrap() {
			Value value = Values.ofText("aaa");
			assertThat(value.unwrap(int.class)).isEmpty();
			assertThat(value.unwrap(String.class)).hasValue("aaa");
			assertThat(value.unwrap(CharSequence.class)).hasValue("aaa");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class NumericValueTest {

		@Mock
		PrintWriter printWriter;

		@Test
		void englishLocale() {
			Values.ofNumeric(1_000_000).print(printWriter, Locale.ENGLISH);
			then(printWriter).should().append("1,000,000");
		}

		@Test
		void italianLocale() {
			Values.ofNumeric(1_000_000).print(printWriter, Locale.ITALIAN);
			then(printWriter).should().append("1.000.000");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.NumericValue.class).verify();
		}

		@Test
		void asString() {
			assertThat(Values.ofNumeric(1)).hasToString("Numeric[1]");
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofNumeric(42).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Numeric[42] to Text[2]");
		}

		@Test
		void unwrap() {
			Value value = Values.ofNumeric(42);
			assertThat(value.unwrap(Integer.class)).isEmpty();
			assertThat(value.unwrap(Long.class)).isPresent().contains(42L);
			assertThat(value.unwrap(String.class)).isPresent().contains("42");
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SizeValueTest {

		@Mock
		PrintWriter printWriter;

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
		void approximateOnPrint(ArgumentsAccessor args) {
			long bytes = args.getLong(0);
			String expectedValue = args.getString(1);
			String expectedUnit = args.getString(2);
			Values.ofSize(bytes).print(printWriter, Locale.ITALIAN);
			then(printWriter).should().append(expectedValue);
			then(printWriter).should().append(expectedUnit);
		}

		@Test
		void appendWithUkLocale() {
			long bytes = 1024 * 2 + 1024 / 2;
			Values.ofSize(bytes).print(printWriter, Locale.UK);
			then(printWriter).should().append("2.5");
			then(printWriter).should().append("KB");
		}

		@Test
		void appendWithItalianLocale() {
			long bytes = 1024 * 2 + 1024 / 2;
			Values.ofSize(bytes).print(printWriter, Locale.ITALIAN);
			then(printWriter).should().append("2,5");
			then(printWriter).should().append("KB");
		}

		@Test
		void asString() {
			assertThat(Values.ofSize(0L)).hasToString("Size[0B]");
			assertThat(Values.ofSize(512L)).hasToString("Size[512B]");
			assertThat(Values.ofSize(1023L)).hasToString("Size[1023B]");
		}

		@Test
		void size() {
			assertThatThrownBy(() -> Values.ofSize(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("negative size");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.SizeValue.class).verify();
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofSize(1000).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Size[1000B] to Text[2]");
		}

		@Test
		void unwrap() {
			Value value = Values.ofSize(10);
			assertThat(value.unwrap(Long.class)).hasValue(10L);
			assertThat(value.unwrap(Integer.class)).isEmpty();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class PathValueTest {

		@Mock
		PrintWriter printWriter;

		@Test
		void appendOk() {
			Values.ofPath(Paths.get(".")).print(printWriter, Locale.getDefault());
			then(printWriter).should().append(".");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.PathValue.class).verify();
		}

		@Test
		void asString() {
			assertThat(Values.ofPath(Paths.get("file"))).hasToString("Path[file]");
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.ofPath(Paths.get("file")).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare Path[file] to Text[2]");
		}

		@Test
		void nullPath() {
			assertThatThrownBy(() -> Values.ofPath(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("path cannot be null");
		}

	}

	@Nested
	class WithStyleTest {

		@Test
		void appendOk() {
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			Value value = Values.withStyle(Values.ofNumeric(1), Ansi.Style.BG_BLUE);
			value.print(printWriter, Locale.getDefault());
			assertThat(writer.toString()).isEqualTo("\u001B[44m1\u001B[49m");
		}

		@Test
		void nullValue() {
			assertThatThrownBy(() -> Values.withStyle(null, Ansi.Style.BG_BLUE))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("value cannot be null");
		}

		@Test
		void nullStyle() {
			assertThatThrownBy(() -> Values.withStyle(Values.ofText("aaa"), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("style cannot be null");
		}

		@Test
		void compareToAnotherValueType() {
			assertThatThrownBy(() -> Values.withStyle(Values.ofNumeric(1), Ansi.Style.BG_BLUE).compareTo(Values.ofText("2")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cannot compare StyledValue[value=Numeric[1],style='BG_BLUE'] to Text[2]");
		}

		@Test
		void equalsContract() {
			EqualsVerifier.forClass(Values.StyledValue.class).verify();
		}

		@Test
		void asString() {
			assertThat(Values.withStyle(Values.ofPath(Paths.get("file")), Ansi.Style.FG_RED)).hasToString("StyledValue[value=Path[file],style='FG_RED']");
		}

		@Test
		void unwrap() {
			Value value = Values.ofPath(Paths.get("."));
			assertThat(value.unwrap(Path.class)).isPresent();
			assertThat(value.unwrap(Integer.class)).isEmpty();
		}
	}

	@Nested
	class SortingBetweenValuesTest {

		@Test
		void instant() {
			List<Value> sorted = Stream.of(
				Values.ofInstant(Instant.ofEpochMilli(100)),
				Values.ofInstant(Instant.ofEpochMilli(-100)),
				Values.ofInstant(Instant.ofEpochMilli(0)))
				.sorted()
				.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.ofInstant(Instant.ofEpochMilli(-100)),
				Values.ofInstant(Instant.ofEpochMilli(0)),
				Values.ofInstant(Instant.ofEpochMilli(100)));
		}

		@Test
		void numeric() {
			List<Value> sorted = Stream.of(
				Values.ofNumeric(1),
				Values.ofNumeric(-1),
				Values.ofNumeric(0))
				.sorted()
				.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.ofNumeric(-1),
				Values.ofNumeric(0),
				Values.ofNumeric(1));
		}

		@Test
		void text() {
			List<Value> sorted = Stream.of(
				Values.ofText("a2"),
				Values.ofText("a10"),
				Values.ofText("a1"))
				.sorted()
				.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.ofText("a1"),
				Values.ofText("a2"),
				Values.ofText("a10"));
		}

		@Test
		void size() {
			List<Value> sorted = Stream.of(
				Values.ofSize(1),
				Values.ofSize(2),
				Values.ofSize(3))
				.sorted()
				.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.ofSize(1),
				Values.ofSize(2),
				Values.ofSize(3));
		}

		@Test
		void path() {
			List<Value> sorted = Stream.of(
				Values.ofPath(Paths.get("a10")),
				Values.ofPath(Paths.get("a1")),
				Values.ofPath(Paths.get("a20")),
				Values.ofPath(Paths.get("a2")))
				.sorted()
				.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.ofPath(Paths.get("a1")),
				Values.ofPath(Paths.get("a2")),
				Values.ofPath(Paths.get("a10")),
				Values.ofPath(Paths.get("a20")));
		}

		@Test
		void duration() {
			List<Value> sorted = Stream.of(
				Values.ofDuration(Duration.ofMillis(1)),
				Values.ofDuration(Duration.ofMillis(2)))
				.sorted()
				.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
				Values.ofDuration(Duration.ofMillis(1)),
				Values.ofDuration(Duration.ofMillis(2)));
		}

	}

	@Nested
	class ComparatorsTest {

		@Nested
		class AlphaNumericStringComparatorTest {

			final Comparator<String> sut = Values.Comparators.alphanum();

			@Test
			void sortLetters() {
				List<String> input = Arrays.asList("b", "c", "a", "ad", "a");
				input.sort(sut);
				assertThat(input).containsExactly("a", "a", "ad", "b", "c");
			}

			@Test
			void sortIntegers() {
				List<String> input = Arrays.asList("2", "20", "10", "1");
				input.sort(sut);
				assertThat(input).containsExactly("1", "2", "10", "20");
			}

			@Test
			void sortDoubles() {
				List<String> input = Arrays.asList("1.0", "1.3", "1.2", "1.1");
				input.sort(sut);
				assertThat(input).containsExactly("1.0", "1.1", "1.2", "1.3");
			}

			@Test
			void sortWithNumberSuffix() {
				List<String> input = Arrays.asList("foo2", "foo20", "foo10", "foo1");
				input.sort(sut);
				assertThat(input).containsExactly("foo1", "foo2", "foo10", "foo20");
			}

			@Test
			void sortEmpty() {
				List<String> input = Arrays.asList("", "");
				input.sort(sut);
				assertThat(input).containsExactly("", "");
			}

			@Test
			void sortEquals() {
				List<String> input = Arrays.asList("a", "a", "a", "a");
				input.sort(sut);
				assertThat(input).containsExactly("a", "a", "a", "a");
			}

			@Test
			void sortDifferentLengths() {
				List<String> input = Arrays.asList("", "a", "a1a", "a1", "a1aaa", "a1aa");
				input.sort(sut);
				assertThat(input).containsExactly("", "a", "a1", "a1a", "a1aa", "a1aaa");
			}

			@Test
			void sortDates() {
				List<String> input = Arrays.asList("20180604", "20180603", "20180602", "20180601");
				input.sort(sut);
				assertThat(input).containsExactly("20180601", "20180602", "20180603", "20180604");
			}

			@Test
			void sortMisc() {
				List<String> input = Arrays.asList("a.1", "1.a", "2.a", "b.1");
				input.sort(sut);
				assertThat(input).containsExactly("1.a", "2.a", "a.1", "b.1");
			}
		}

		@Nested
		class NoneLastTest {

			final Comparator<Value> sut = Values.Comparators.noneLast(Comparator.naturalOrder());

			@Test
			void noneLast() {
				List<Value> values = Stream.of(
					Values.ofNumeric(1),
					Values.ofNumeric(-1),
					Values.none(),
					Values.ofNumeric(0)
				).collect(Collectors.toList());

				values.sort(sut);

				assertThat(values).last().isEqualTo(Values.none());
			}
		}

		@Nested
		class NoneFirstTest {

			Comparator<Value> sut = Values.Comparators.noneFirst(Comparator.naturalOrder());

			@Test
			void noneFirst() {
				List<Value> values = Stream.of(
					Values.ofNumeric(1),
					Values.ofNumeric(-1),
					Values.none(),
					Values.ofNumeric(0)
				).collect(Collectors.toList());

				values.sort(sut);

				assertThat(values).first().isEqualTo(Values.none());
			}
		}
	}
}
