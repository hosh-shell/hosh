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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hosh.spi.Values.AlphaNumericStringComparator;
import org.hosh.spi.ValuesTest.AlphaNumericStringComparatorTest;
import org.hosh.spi.ValuesTest.LocalPathValueTest;
import org.hosh.spi.ValuesTest.NoneValueTest;
import org.hosh.spi.ValuesTest.NumericValueTest;
import org.hosh.spi.ValuesTest.SizeValueTest;
import org.hosh.spi.ValuesTest.SortingBetweenValuesTest;
import org.hosh.spi.ValuesTest.TextValueTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import nl.jqno.equalsverifier.EqualsVerifier;

@RunWith(Suite.class)
@SuiteClasses({
		NoneValueTest.class,
		TextValueTest.class,
		NumericValueTest.class,
		SizeValueTest.class,
		LocalPathValueTest.class,
		AlphaNumericStringComparatorTest.class,
		SortingBetweenValuesTest.class
})
public class ValuesTest {
	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class NoneValueTest {
		@Mock
		private Appendable appendable;

		@Test
		public void append() throws IOException {
			Values.none().append(appendable, Locale.ENGLISH);
			then(appendable).should().append("");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.None.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.none()).hasToString("None");
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class TextValueTest {
		@Mock
		private Appendable appendable;

		@Test
		public void nullIsReject() {
			assertThatThrownBy(() -> Values.ofText(null))
					.hasMessage("text cannot be null")
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		public void appendOk() throws IOException {
			Values.ofText("aaa").append(appendable, Locale.getDefault());
			then(appendable).should().append("aaa");
		}

		@Test(expected = UncheckedIOException.class)
		public void appendError() throws IOException {
			given(appendable.append(ArgumentMatchers.any())).willThrow(IOException.class);
			Values.ofText("aaa").append(appendable, Locale.getDefault());
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.Text.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofText("aaa")).hasToString("Text[aaa]");
		}

		@Test
		public void matches() {
			Value text = Values.ofText("aaabaaa");
			assertThat(text.matches("a+ba+")).isTrue();
			assertThat(text.matches(".*b.*")).isTrue();
			assertThat(text.matches(".*c.*")).isFalse();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class NumericValueTest {
		@Mock
		private Appendable appendable;

		@Test
		public void englishLocale() throws IOException {
			Values.ofNumeric(1_000_000).append(appendable, Locale.ENGLISH);
			then(appendable).should().append("1,000,000");
		}

		@Test
		public void italianLocale() throws IOException {
			Values.ofNumeric(1_000_000).append(appendable, Locale.ITALIAN);
			then(appendable).should().append("1.000.000");
		}

		@Test(expected = UncheckedIOException.class)
		public void appendError() throws IOException {
			given(appendable.append(ArgumentMatchers.any())).willThrow(IOException.class);
			Values.ofNumeric(1_000_000).append(appendable, Locale.getDefault());
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.Numeric.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofNumeric(1)).hasToString("Numeric[1]");
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SizeValueTest {
		@Mock
		private Appendable appendable;

		@Test
		public void appendWithUkLocale() throws IOException {
			Values.ofHumanizedSize(2 * Values.KIB + Values.KIB / 2).append(appendable, Locale.UK);
			then(appendable).should().append("2.5");
			then(appendable).should().append("KB");
		}

		@Test
		public void appendWithItalianLocale() throws IOException {
			Values.ofHumanizedSize(2 * Values.KIB + Values.KIB / 2).append(appendable, Locale.ITALIAN);
			then(appendable).should().append("2,5");
			then(appendable).should().append("KB");
		}

		@Test(expected = UncheckedIOException.class)
		public void appendError() throws IOException {
			given(appendable.append(ArgumentMatchers.any())).willThrow(IOException.class);
			Values.ofHumanizedSize(10).append(appendable, Locale.getDefault());
		}

		@Test
		public void humanizedSizeApproximation() {
			long twoMegabytes = Values.KIB * Values.KIB * 2;
			assertThat(Values.ofHumanizedSize(twoMegabytes - 1)).hasToString("Size[2.0MB]");
		}

		@Test
		public void humanizedSize() {
			assertThat(Values.ofHumanizedSize(0L)).hasToString("Size[0B]");
			assertThat(Values.ofHumanizedSize(512L)).hasToString("Size[512B]");
			assertThat(Values.ofHumanizedSize(1023L)).hasToString("Size[1023B]");
			assertThat(Values.ofHumanizedSize(1024L)).hasToString("Size[1.0KB]");
			assertThat(Values.ofHumanizedSize(1024L * 1024)).hasToString("Size[1.0MB]");
			assertThat(Values.ofHumanizedSize(1024L * 1024 * 1024)).hasToString("Size[1.0GB]");
			assertThatThrownBy(() -> Values.ofHumanizedSize(-1))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("negative size");
		}

		@Test
		public void size() {
			assertThatThrownBy(() -> Values.ofHumanizedSize(-1))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("negative size");
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.Size.class).verify();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class LocalPathValueTest {
		@Mock
		private Appendable appendable;

		@Test
		public void appendOk() throws IOException {
			Values.ofLocalPath(Paths.get(".")).append(appendable, Locale.getDefault());
			then(appendable).should().append(".");
		}

		@Test(expected = UncheckedIOException.class)
		public void appendError() throws IOException {
			given(appendable.append(ArgumentMatchers.any())).willThrow(IOException.class);
			Values.ofLocalPath(Paths.get(".")).append(appendable, Locale.getDefault());
		}

		@Test
		public void equalsContract() {
			EqualsVerifier.forClass(Values.LocalPath.class).verify();
		}

		@Test
		public void asString() {
			assertThat(Values.ofLocalPath(Paths.get("file"))).hasToString("LocalPath[file]");
		}
	}

	public static class AlphaNumericStringComparatorTest {
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
			List<String> input = Arrays.asList("", "", "", "");
			input.sort(new AlphaNumericStringComparator());
			assertThat(input).containsExactly("", "", "", "");
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

	public static class SortingBetweenValuesTest {
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
					Values.ofHumanizedSize(1),
					Values.none(),
					Values.none(),
					Values.ofHumanizedSize(2),
					Values.ofHumanizedSize(3))
					.sorted()
					.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
					Values.none(),
					Values.none(),
					Values.ofHumanizedSize(1),
					Values.ofHumanizedSize(2),
					Values.ofHumanizedSize(3));
		}

		@Test
		public void localPathWithNone() {
			List<Value> sorted = Stream.of(
					Values.ofLocalPath(Paths.get("bbb")),
					Values.none(),
					Values.none(),
					Values.ofLocalPath(Paths.get("aaa")),
					Values.ofLocalPath(Paths.get("ccc")))
					.sorted()
					.collect(Collectors.toList());
			assertThat(sorted).containsExactly(
					Values.none(),
					Values.none(),
					Values.ofLocalPath(Paths.get("aaa")),
					Values.ofLocalPath(Paths.get("bbb")),
					Values.ofLocalPath(Paths.get("ccc")));
		}
	}
}
