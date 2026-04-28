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

import hosh.test.support.WithTimeZone;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValuesTest {

	@Nested
	class NoneValueTest {

		@Test
		void show() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.none().show(Locale.ENGLISH)).isEmpty();
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.None.class).verify();
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.none();
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare None with Text[2]");
		}

		@Test
		void compareToNone() {
			// Given
			Value v1 = Values.none();
			Value v2 = Values.none();
			// When / Then
			assertThat(v1).usingDefaultComparator().isEqualByComparingTo(v2);
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.none()).hasToString("None");
		}

		@Test
		void unwrap() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.none().unwrap(Object.class)).isEmpty();
		}

		@Test
		void merge() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.none().merge(Values.ofText("a"))).isNotEmpty();
			assertThat(Values.none().merge(Values.none())).isEmpty();
		}
	}

	@Nested
	class DurationValueTest {

		@Test
		void show() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofDuration(Duration.ofMillis(1)).show(Locale.ENGLISH)).isEqualTo("PT0.001S");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.DurationValue.class).verify();
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofDuration(Duration.ofMillis(1))).hasToString("Duration[PT0.001S]");
		}

		@Test
		void nullDuration() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.ofDuration(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("duration cannot be null");
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.ofDuration(Duration.ofHours(1));
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare Duration[PT1H] with Text[2]");
		}

		@Test
		void compareToSameValueType() {
			// Given
			Value a = Values.ofDuration(Duration.ofHours(1));
			Value b = Values.ofDuration(Duration.ofHours(2));
			Value c = Values.ofDuration(Duration.ofHours(1));
			// When / Then
			assertThat(a.compareTo(b)).isEqualTo(-1);
			assertThat(b.compareTo(a)).isEqualTo(1);
			assertThat(a.compareTo(c)).isEqualTo(0);
		}
	}

	@Nested
	class InstantValueTest {

		@RegisterExtension
		final WithTimeZone withTimeZone = new WithTimeZone();

		@Test
		void show() {
			// Given
			withTimeZone.changeTo(TimeZone.getTimeZone("Europe/Zurich"));
			// When / Then
			assertThat(Values.ofInstant(Instant.EPOCH).show(Locale.ENGLISH)).isEqualTo("1970-01-01T01:00:00");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.InstantValue.class).verify();
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofInstant(Instant.EPOCH)).hasToString("Instant[1970-01-01T00:00:00Z]");
		}

		@Test
		void nullDuration() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.ofInstant(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("instant cannot be null");
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.ofInstant(Instant.EPOCH);
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare Instant[1970-01-01T00:00:00Z] with Text[2]");
		}
	}

	@Nested
	class TextValueTest {

		@Test
		void nullIsReject() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.ofText(null))
					.hasMessage("text cannot be null")
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void show() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofText("aaa").show(Locale.getDefault())).isEqualTo("aaa");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.TextValue.class)
					.verify();
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofText("aaa")).hasToString("Text[aaa]");
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.ofText("2");
			Value b = Values.ofDuration(Duration.ofHours(1));
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare Text[2] with Duration[PT1H]");
		}

		@Test
		void nullText() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.ofText(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("text cannot be null");
		}

		@Test
		void unwrap() {
			// Given
			Value value = Values.ofText("aaa");
			// When / Then
			assertThat(value.unwrap(int.class)).isEmpty();
			assertThat(value.unwrap(String.class)).hasValue("aaa");
			assertThat(value.unwrap(CharSequence.class)).hasValue("aaa");
		}
	}

	@Nested
	class NumericValueTest {

		@Test
		void showWithEnglishLocale() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofNumeric(1_000_000).show(Locale.ENGLISH)).isEqualTo("1,000,000");
		}

		@Test
		void showWithItalianLocale() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofNumeric(1_000_000).show(Locale.ITALIAN)).isEqualTo("1.000.000");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.NumericValue.class).verify();
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofNumeric(1)).hasToString("Numeric[1]");
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.ofNumeric(42);
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare Numeric[42] with Text[2]");
		}

		@Test
		void unwrap() {
			// Given
			Value value = Values.ofNumeric(42);
			// When / Then
			assertThat(value.unwrap(Integer.class)).isEmpty();
			assertThat(value.unwrap(Long.class)).isPresent().contains(42L);
			assertThat(value.unwrap(String.class)).isPresent().contains("42");
		}

	}

	@Nested
	class SizeValueTest {

		@ParameterizedTest
		@CsvSource({
				"            0,      0B",
				"            1,      1B",
				"            2,      2B",
				"           10,     10B",
				"         1023,   1023B",
				"         1024,     1KB",
				"         1025,     1KB",
				"         1026,     1KB",
				"         1027,     1KB",
				"         1028,     1KB",
				"         2047,     2KB",
				"         2048,     2KB",
				"         4096,     4KB",
				"         8192,     8KB",
				"        16384,    16KB",
				"      1048576,     1MB",
				"      2097152,     2MB",
				"     11048576,  10.5MB",
				"    134217728,   128MB",
				"    199999999, 190.7MB",
				"    200000001, 190.7MB",
				"    200001000, 190.7MB",
				"    201000000, 191.7MB",
				"    205000000, 195.5MB",
				"    210000000, 200.3MB",
				"   1073741824,     1GB",
				"  17179869184,    16GB",
				" 274877906944,   256GB",
				"1099511627780,     1TB"
		})
		void approximateOnPrint(long bytes, String expectedValue) {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofSize(bytes).show(Locale.US)).isEqualTo(expectedValue);
		}

		@Test
		void showWithUkLocale() {
			// Given
			long bytes = 1024 * 2 + 1024 / 2;
			// When
			String result = Values.ofSize(bytes).show(Locale.UK);
			// Then
			assertThat(result).isEqualTo("2.5KB");
		}

		@Test
		void showWithItalianLocale() {
			// Given
			long bytes = 1024 * 2 + 1024 / 2;
			// When
			String result = Values.ofSize(bytes).show(Locale.ITALIAN);
			// Then
			assertThat(result).isEqualTo("2,5KB");
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofSize(0L)).hasToString("Size[0B]");
			assertThat(Values.ofSize(512L)).hasToString("Size[512B]");
			assertThat(Values.ofSize(1023L)).hasToString("Size[1023B]");
		}

		@Test
		void size() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.ofSize(-1))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("negative size");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.SizeValue.class).verify();
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.ofSize(1000);
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare Size[1000B] with Text[2]");
		}

		@Test
		void compareToSameValueType() {
			// Given
			Value a = Values.ofSize(100);
			Value b = Values.ofSize(10);
			Value c = Values.ofSize(100);
			// When / Then
			assertThat(a.compareTo(b)).isEqualTo(1);
			assertThat(b.compareTo(a)).isEqualTo(-1);
			assertThat(a.compareTo(c)).isEqualTo(0);
		}

		@Test
		void unwrap() {
			// Given
			Value value = Values.ofSize(10);
			// When / Then
			assertThat(value.unwrap(Long.class)).hasValue(10L);
			assertThat(value.unwrap(Integer.class)).isEmpty();
		}
	}

	@Nested
	class PathValueTest {

		@Test
		void show() {
			// Given
			// (no setup)
			// When
			String result = Values.ofPath(Paths.get(".")).show(Locale.getDefault());
			// Then
			assertThat(result).isEqualTo(".");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.PathValue.class).verify();
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofPath(Paths.get("file"))).hasToString("Path[file]");
		}

		@Test
		void compareTo() {
			// Given
			Value a = Values.ofPath(Paths.get("file"));
			Value b = Values.ofPath(Paths.get("another_file"));
			// When / Then
			assertThat(a).isEqualByComparingTo(a);
			assertThat(a).isNotEqualByComparingTo(b);
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.ofPath(Paths.get("file"));
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare Path[file] with Text[2]");
		}

		@Test
		void nullPath() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.ofPath(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("path cannot be null");
		}

		@Test
		void unwrap() {
			// Given
			Value path = Values.ofPath(Paths.get("file"));
			// When / Then
			assertThat(path.unwrap(Path.class)).hasValue(Paths.get("file"));
			assertThat(path.unwrap(String.class)).hasValue("file");
			assertThat(path.unwrap(Integer.class)).isEmpty();
		}

	}

	@Nested
	class BytesTest {

		@Test
		void show() {
			// Given
			Value value = Values.ofBytes(new byte[]{-1, 1, 127});
			// When
			String result = value.show(Locale.getDefault());
			// Then
			assertThat(result).isEqualTo("ff:01:7f");
		}

		@Test
		void nullValue() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.ofBytes(null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("bytes cannot be null");
		}

		@Test
		void compareTo() {
			// Given
			Value a = Values.ofBytes(new byte[]{-1, -1});
			Value b = Values.ofBytes(new byte[0]);
			// When / Then
			assertThat(a).isEqualByComparingTo(a);
			assertThat(a).isNotEqualByComparingTo(b);
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.ofBytes(new byte[]{-1, -1});
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare Bytes[[-1, -1]] with Text[2]");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.BytesValue.class).verify();
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.ofBytes(new byte[]{-1, -1})).hasToString("Bytes[[-1, -1]]");
		}


	}

	@Nested
	class WithStyleTest {

		@Test
		void show() {
			// Given
			Value value = Values.withStyle(Values.ofNumeric(1), Ansi.Style.BG_BLUE);
			// When
			String result = value.show(Locale.getDefault());
			// Then
			assertThat(result).isEqualTo("\u001B[44m1\u001B[49m");
		}

		@Test
		void nullValue() {
			// Given
			// (no setup)
			// When / Then
			assertThatThrownBy(() -> Values.withStyle(null, Ansi.Style.BG_BLUE))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("value cannot be null");
		}

		@Test
		void nullStyle() {
			// Given
			Value text = Values.ofText("text");
			// When / Then
			assertThatThrownBy(() -> Values.withStyle(text, null))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("style cannot be null");
		}

		@Test
		void compareTo() {
			// Given
			Value a = Values.withStyle(Values.ofNumeric(1), Ansi.Style.BG_BLUE);
			Value b = Values.withStyle(Values.ofNumeric(2), Ansi.Style.BG_BLUE);
			// When / Then
			assertThat(a).isEqualByComparingTo(a);
			assertThat(a).isNotEqualByComparingTo(b);
		}

		@Test
		void compareToAnotherValueType() {
			// Given
			Value a = Values.withStyle(Values.ofNumeric(1), Ansi.Style.BG_BLUE);
			Value b = Values.ofText("2");
			// When / Then
			assertThatThrownBy(() -> a.compareTo(b))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("cannot compare StyledValue[value=Numeric[1],style='BG_BLUE'] with Text[2]");
		}

		@Test
		void equalsContract() {
			// Given
			// (no setup)
			// When / Then
			EqualsVerifier.forClass(Values.StyledValue.class).verify();
		}

		@Test
		void asString() {
			// Given
			// (no setup)
			// When / Then
			assertThat(Values.withStyle(Values.ofPath(Paths.get("file")), Ansi.Style.FG_RED)).hasToString("StyledValue[value=Path[file],style='FG_RED']");
		}

		@Test
		void unwrap() {
			// Given
			Value value = Values.withStyle(Values.ofPath(Paths.get(".")), Ansi.Style.FG_RED);
			// When / Then
			assertThat(value.unwrap(Path.class)).isPresent();
			assertThat(value.unwrap(Integer.class)).isEmpty();
		}
	}

	@Nested
	class SortingBetweenValuesTest {

		@Test
		void instant() {
			// Given
			// (no setup)
			// When
			List<Value> sorted = Stream.of(
							Values.ofInstant(Instant.ofEpochMilli(100)),
							Values.ofInstant(Instant.ofEpochMilli(-100)),
							Values.ofInstant(Instant.ofEpochMilli(0)))
					.sorted()
					.toList();
			// Then
			assertThat(sorted).containsExactly(
					Values.ofInstant(Instant.ofEpochMilli(-100)),
					Values.ofInstant(Instant.ofEpochMilli(0)),
					Values.ofInstant(Instant.ofEpochMilli(100)));
		}

		@Test
		void numeric() {
			// Given
			// (no setup)
			// When
			List<Value> sorted = Stream.of(
							Values.ofNumeric(1),
							Values.ofNumeric(-1),
							Values.ofNumeric(0))
					.sorted()
					.toList();
			// Then
			assertThat(sorted).containsExactly(
					Values.ofNumeric(-1),
					Values.ofNumeric(0),
					Values.ofNumeric(1));
		}

		@Test
		void text() {
			// Given
			// (no setup)
			// When
			List<Value> sorted = Stream.of(
							Values.ofText("a2"),
							Values.ofText("a10"),
							Values.ofText("a1"))
					.sorted()
					.toList();
			// Then
			assertThat(sorted).containsExactly(
					Values.ofText("a1"),
					Values.ofText("a2"),
					Values.ofText("a10"));
		}

		@Test
		void size() {
			// Given
			// (no setup)
			// When
			List<Value> sorted = Stream.of(
							Values.ofSize(1),
							Values.ofSize(2),
							Values.ofSize(3))
					.sorted()
					.toList();
			// Then
			assertThat(sorted).containsExactly(
					Values.ofSize(1),
					Values.ofSize(2),
					Values.ofSize(3));
		}

		@Test
		void path() {
			// Given
			// (no setup)
			// When
			List<Value> sorted = Stream.of(
							Values.ofPath(Paths.get("a10")),
							Values.ofPath(Paths.get("a1")),
							Values.ofPath(Paths.get("a20")),
							Values.ofPath(Paths.get("a2")))
					.sorted()
					.toList();
			// Then
			assertThat(sorted).containsExactly(
					Values.ofPath(Paths.get("a1")),
					Values.ofPath(Paths.get("a2")),
					Values.ofPath(Paths.get("a10")),
					Values.ofPath(Paths.get("a20")));
		}

		@Test
		void duration() {
			// Given
			// (no setup)
			// When
			List<Value> sorted = Stream.of(
							Values.ofDuration(Duration.ofMillis(1)),
							Values.ofDuration(Duration.ofMillis(2)))
					.sorted()
					.toList();
			// Then
			assertThat(sorted).containsExactly(
					Values.ofDuration(Duration.ofMillis(1)),
					Values.ofDuration(Duration.ofMillis(2)));
		}

	}

	@Nested
	class ComparatorsTest {

		@Nested
		class NaturalSortOrderComparatorTest {

			final Comparator<String> sut = Values.Comparators.naturalSortOrder();

			@Test
			void sortLetters() {
				// Given
				List<String> input = Arrays.asList("b", "c", "a", "ad", "a");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("a", "a", "ad", "b", "c");
			}

			@Test
			void sortIntegers() {
				// Given
				List<String> input = Arrays.asList("2", "20", "10", "1");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("1", "2", "10", "20");
			}

			@Test
			void sortDoubles() {
				// Given
				List<String> input = Arrays.asList("1.0", "1.3", "1.2", "1.1");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("1.0", "1.1", "1.2", "1.3");
			}

			@Test
			void sortWithNumberSuffix() {
				// Given
				List<String> input = Arrays.asList("foo2", "foo20", "foo10", "foo1");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("foo1", "foo2", "foo10", "foo20");
			}

			@Test
			void sortEmpty() {
				// Given
				List<String> input = Arrays.asList("", "");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("", "");
			}

			@Test
			void sortEquals() {
				// Given
				List<String> input = Arrays.asList("a", "a", "a", "a");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("a", "a", "a", "a");
			}

			@Test
			void sortDifferentLengths() {
				// Given
				List<String> input = Arrays.asList("", "a", "a1a", "a1", "a1aaa", "a1aa");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("", "a", "a1", "a1a", "a1aa", "a1aaa");
			}

			@Test
			void sortDates() {
				// Given
				List<String> input = Arrays.asList("20180604", "20180603", "20180602", "20180601");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("20180601", "20180602", "20180603", "20180604");
			}

			@Test
			void sortMisc() {
				// Given
				List<String> input = Arrays.asList("a.1", "1.a", "2.a", "b.1");
				// When
				input.sort(sut);
				// Then
				assertThat(input).containsExactly("1.a", "2.a", "a.1", "b.1");
			}
		}

		@Nested
		class NoneLastTest {

			final Comparator<Value> sut = Values.Comparators.noneLast(Comparator.naturalOrder());

			@Property
			void noneLast(@ForAll("listOfNumericValuesContainingNone") List<Value> candidate) {
				// Given
				// (no setup)
				// When / Then
				candidate.sort(sut);
				assertThat(candidate).last().isEqualTo(Values.none());
			}
		}

		@Nested
		class NoneFirstTest {

			final Comparator<Value> sut = Values.Comparators.noneFirst(Comparator.naturalOrder());

			@Property
			void noneFirst(@ForAll("listOfNumericValuesContainingNone") List<Value> candidate) {
				// Given
				// (no setup)
				// When / Then
				candidate.sort(sut);
				assertThat(candidate).first().isEqualTo(Values.none());
			}
		}

		// creates random lists of values with 3 None elements
		@Provide
		Arbitrary<List<Value>> listOfNumericValuesContainingNone() {
			Arbitrary<List<Value>> numericValues = Arbitraries.integers()
					.map(Values::ofNumeric)
					.list().ofMinSize(1).ofMaxSize(20);
			return numericValues.flatMap(base ->
					Arbitraries.integers().between(0, base.size() + 2).map(randomIndex -> {
						List<Value> mutable = new ArrayList<>(base);
						mutable.addFirst(Values.none());
						mutable.add(Values.none());
						mutable.add(Math.min(randomIndex, mutable.size()), Values.none());
						return mutable;
					}));
		}

	}

	@Nested
	class MergeTest {

		@Test
		void mergeNumericValues() {
			// Given
			var none = Values.none();
			var num = Values.ofNumeric(1);
			// When / Then
			assertThat(none.merge(none)).isEmpty();
			assertThat(num.merge(num)).hasValue(Values.ofNumeric(2));
			assertThat(none.merge(num)).hasValue(num);
			assertThat(num.merge(none)).hasValue(num);
		}


		@Test
		void mergeSizeValues() {
			// Given
			var none = Values.none();
			var size = Values.ofSize(1);
			// When / Then
			assertThat(none.merge(none)).isEmpty();
			assertThat(size.merge(size)).hasValue(Values.ofSize(2));
			assertThat(none.merge(size)).hasValue(size);
			assertThat(size.merge(none)).hasValue(size);
		}
	}

}
