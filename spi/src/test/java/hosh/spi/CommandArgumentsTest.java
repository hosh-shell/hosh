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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class CommandArgumentsTest {

	@Nested
	class Creation {

		@Test
		void ofWithNoArguments() {
			// Given
			// When
			CommandArguments sut = CommandArguments.of();

			// Then
			assertThat(sut.isEmpty()).isTrue();
			assertThat(sut.size()).isZero();
		}

		@Test
		void ofWithSingleArgument() {
			// Given
			// When
			CommandArguments sut = CommandArguments.of("test");

			// Then
			assertThat(sut.isEmpty()).isFalse();
			assertThat(sut.size()).isOne();
			assertThat(sut.get(0).asString()).isEqualTo("test");
		}

		@Test
		void ofWithMultipleArguments() {
			// Given
			// When
			CommandArguments sut = CommandArguments.of("arg1", "arg2", "arg3");

			// Then
			assertThat(sut.size()).isEqualTo(3);
			assertThat(sut.get(0).asString()).isEqualTo("arg1");
			assertThat(sut.get(1).asString()).isEqualTo("arg2");
			assertThat(sut.get(2).asString()).isEqualTo("arg3");
		}

		@Test
		void ofFromListIsDefensive() {
			// Given
			List<CommandArguments.CommandArgument> original = List.of(
				new CommandArguments.CommandArgument("arg1"),
				new CommandArguments.CommandArgument("arg2")
			);

			// When
			CommandArguments sut = CommandArguments.of(original);

			// Then
			assertThat(sut.size()).isEqualTo(2);
			assertThat(sut.get(0).asString()).isEqualTo("arg1");
		}
	}

	@Nested
	class Size {

		@Test
		void sizeWithEmptyArguments() {
			// Given
			CommandArguments sut = CommandArguments.of();

			// When
			int size = sut.size();

			// Then
			assertThat(size).isZero();
		}

		@Test
		void sizeWithMultipleArguments() {
			// Given
			CommandArguments sut = CommandArguments.of("a", "b", "c", "d");

			// When
			int size = sut.size();

			// Then
			assertThat(size).isEqualTo(4);
		}
	}

	@Nested
	class IsEmpty {

		@Test
		void isEmptyWhenNoArguments() {
			// Given
			CommandArguments sut = CommandArguments.of();

			// When
			// Then
			assertThat(sut.isEmpty()).isTrue();
		}

		@Test
		void isNotEmptyWhenHasArguments() {
			// Given
			CommandArguments sut = CommandArguments.of("test");

			// When
			// Then
			assertThat(sut.isEmpty()).isFalse();
		}
	}

	@Nested
	class Get {

		@Test
		void getFirstArgument() {
			// Given
			CommandArguments sut = CommandArguments.of("first", "second");

			// When
			CommandArguments.CommandArgument result = sut.get(0);

			// Then
			assertThat(result.asString()).isEqualTo("first");
		}

		@Test
		void getLastArgument() {
			// Given
			CommandArguments sut = CommandArguments.of("first", "second", "third");

			// When
			CommandArguments.CommandArgument result = sut.get(2);

			// Then
			assertThat(result.asString()).isEqualTo("third");
		}

		@Test
		void getThrowsOnNegativeIndex() {
			// Given
			CommandArguments sut = CommandArguments.of("test");

			// When
			// Then
			assertThatThrownBy(() -> sut.get(-1))
				.isInstanceOf(IndexOutOfBoundsException.class);
		}

		@Test
		void getThrowsOnOutOfBoundsIndex() {
			// Given
			CommandArguments sut = CommandArguments.of("test");

			// When
			// Then
			assertThatThrownBy(() -> sut.get(10))
				.isInstanceOf(IndexOutOfBoundsException.class);
		}
	}

	@Nested
	class Stream {

		@Test
		void streamIsEmpty() {
			// Given
			CommandArguments sut = CommandArguments.of();

			// When
			long count = sut.stream().count();

			// Then
			assertThat(count).isZero();
		}

		@Test
		void streamCollectsAllArguments() {
			// Given
			CommandArguments sut = CommandArguments.of("a", "b", "c");

			// When
			List<String> strings = sut.stream()
				.map(CommandArguments.CommandArgument::asString)
				.collect(Collectors.toList());

			// Then
			assertThat(strings).containsExactly("a", "b", "c");
		}
	}

	@Nested
	class IteratorBehavior {

		@Test
		void iteratorIsEmpty() {
			// Given
			CommandArguments sut = CommandArguments.of();

			// When
			java.util.Iterator<CommandArguments.CommandArgument> iterator = sut.iterator();

			// Then
			assertThat(iterator.hasNext()).isFalse();
		}

		@Test
		void iteratorTraversesAllArguments() {
			// Given
			CommandArguments sut = CommandArguments.of("x", "y", "z");

			// When
			List<String> values = new java.util.ArrayList<>();
			for (CommandArguments.CommandArgument arg : sut) {
				values.add(arg.asString());
			}

			// Then
			assertThat(values).containsExactly("x", "y", "z");
		}
	}

	@Nested
	class CommandArgumentAsString {

		@Test
		void asStringReturnsValue() {
			// Given
			CommandArguments sut = CommandArguments.of("hello world");

			// When
			String result = sut.get(0).asString();

			// Then
			assertThat(result).isEqualTo("hello world");
		}

		@Test
		void asStringWithSpecialCharacters() {
			// Given
			CommandArguments sut = CommandArguments.of("foo!@#$%^&*()");

			// When
			String result = sut.get(0).asString();

			// Then
			assertThat(result).isEqualTo("foo!@#$%^&*()");
		}
	}

	@Nested
	class CommandArgumentAsKey {

		@Test
		void asKeyConvertsValue() {
			// Given
			CommandArguments sut = CommandArguments.of("mykey");

			// When
			Key result = sut.get(0).asKey();

			// Then
			assertThat(result).isEqualTo(Keys.of("mykey"));
		}

		@Test
		void asKeyHandlesUppercaseConversion() {
			// Given
			CommandArguments sut = CommandArguments.of("MyKey");

			// When
			Key result = sut.get(0).asKey();

			// Then
			assertThat(result).isEqualTo(Keys.of("MyKey"));
		}
	}

	@Nested
	class CommandArgumentAsLong {

		@Test
		void asLongWithValidPositiveNumber() {
			// Given
			CommandArguments sut = CommandArguments.of("42");

			// When
			OptionalLong result = sut.get(0).asLong();

			// Then
			assertThat(result).hasValue(42L);
		}

		@Test
		void asLongWithValidNegativeNumber() {
			// Given
			CommandArguments sut = CommandArguments.of("-100");

			// When
			OptionalLong result = sut.get(0).asLong();

			// Then
			assertThat(result).hasValue(-100L);
		}

		@Test
		void asLongWithZero() {
			// Given
			CommandArguments sut = CommandArguments.of("0");

			// When
			OptionalLong result = sut.get(0).asLong();

			// Then
			assertThat(result).hasValue(0L);
		}

		@Test
		void asLongWithMaxValue() {
			// Given
			CommandArguments sut = CommandArguments.of(String.valueOf(Long.MAX_VALUE));

			// When
			OptionalLong result = sut.get(0).asLong();

			// Then
			assertThat(result).hasValue(Long.MAX_VALUE);
		}

		@Test
		void asLongWithMinValue() {
			// Given
			CommandArguments sut = CommandArguments.of(String.valueOf(Long.MIN_VALUE));

			// When
			OptionalLong result = sut.get(0).asLong();

			// Then
			assertThat(result).hasValue(Long.MIN_VALUE);
		}

		@ParameterizedTest
		@ValueSource(strings = {"abc", "12.34", "", "1L", "12abc"})
		void asLongWithInvalidValues(String input) {
			// Given
			CommandArguments sut = CommandArguments.of(input);

			// When
			OptionalLong result = sut.get(0).asLong();

			// Then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class CommandArgumentAsInt {

		@Test
		void asIntWithValidPositiveNumber() {
			// Given
			CommandArguments sut = CommandArguments.of("42");

			// When
			OptionalInt result = sut.get(0).asInt();

			// Then
			assertThat(result).hasValue(42);
		}

		@Test
		void asIntWithValidNegativeNumber() {
			// Given
			CommandArguments sut = CommandArguments.of("-100");

			// When
			OptionalInt result = sut.get(0).asInt();

			// Then
			assertThat(result).hasValue(-100);
		}

		@Test
		void asIntWithZero() {
			// Given
			CommandArguments sut = CommandArguments.of("0");

			// When
			OptionalInt result = sut.get(0).asInt();

			// Then
			assertThat(result).hasValue(0);
		}

		@Test
		void asIntWithMaxValue() {
			// Given
			CommandArguments sut = CommandArguments.of(String.valueOf(Integer.MAX_VALUE));

			// When
			OptionalInt result = sut.get(0).asInt();

			// Then
			assertThat(result).hasValue(Integer.MAX_VALUE);
		}

		@Test
		void asIntWithMinValue() {
			// Given
			CommandArguments sut = CommandArguments.of(String.valueOf(Integer.MIN_VALUE));

			// When
			OptionalInt result = sut.get(0).asInt();

			// Then
			assertThat(result).hasValue(Integer.MIN_VALUE);
		}

		@Test
		void asIntWithLongValueThatOverflows() {
			// Given
			CommandArguments sut = CommandArguments.of(String.valueOf(Long.MAX_VALUE));

			// When
			OptionalInt result = sut.get(0).asInt();

			// Then
			assertThat(result).isEmpty();
		}

		@ParameterizedTest
		@ValueSource(strings = {"abc", "12.34", "", "1I", "12abc"})
		void asIntWithInvalidValues(String input) {
			// Given
			CommandArguments sut = CommandArguments.of(input);

			// When
			OptionalInt result = sut.get(0).asInt();

			// Then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class DurationArgument {

		@Test
		void iso8601FormatUppercase() {
			// Given
			CommandArguments sut = CommandArguments.of("PT5S");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void iso8601FormatLowercase() {
			// Given
			CommandArguments sut = CommandArguments.of("PT5s");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void ourCustomFormatLowercase() {
			// Given
			CommandArguments sut = CommandArguments.of("5s");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void ourCustomFormatUppercase() {
			// Given
			CommandArguments sut = CommandArguments.of("5S");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofSeconds(5));
		}

		@Test
		void emptyValue() {
			// Given
			CommandArguments sut = CommandArguments.of("");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).isEmpty();
		}

		@Test
		void missingUnit() {
			// Given
			CommandArguments sut = CommandArguments.of("5");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).isEmpty();
		}

		@Test
		void durationWithMinutes() {
			// Given
			CommandArguments sut = CommandArguments.of("10m");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofMinutes(10));
		}

		@Test
		void durationWithHours() {
			// Given
			CommandArguments sut = CommandArguments.of("2h");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofHours(2));
		}

		@Test
		void iso8601ComplexFormat() {
			// Given
			CommandArguments sut = CommandArguments.of("PT1H30M");

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).contains(Duration.ofMinutes(90));
		}

		@ParameterizedTest
		@ValueSource(strings = {"invalid", "5x", "xyz", "PT"})
		void invalidDurationFormats(String input) {
			// Given
			CommandArguments sut = CommandArguments.of(input);

			// When
			Optional<Duration> result = sut.get(0).asDuration();

			// Then
			assertThat(result).isEmpty();
		}
	}

	@Nested
	class CommandArgumentOf {

		@Test
		void factoryMethodCreatesCommandArgument() {
			// Given
			// When
			CommandArguments.CommandArgument sut = CommandArguments.CommandArgument.of("test");

			// Then
			assertThat(sut.asString()).isEqualTo("test");
		}

		@Test
		void factoryMethodWithEmptyString() {
			// Given
			// When
			CommandArguments.CommandArgument sut = CommandArguments.CommandArgument.of("");

			// Then
			assertThat(sut.asString()).isEmpty();
		}
	}

	@Nested
	class CommandArgumentAsPath {

		@Test
		void asPathWithRelativePath() {
			// Given
			CommandArguments sut = CommandArguments.of("test.txt");
			State state = Mockito.mock(State.class);
			Path cwd = Paths.get("/home/user");
			when(state.getCwd()).thenReturn(cwd);

			// When
			Path result = sut.get(0).asPath(state);

			// Then
			assertThat(result).isAbsolute();
			assertThat(result.toString()).endsWith("test.txt");
		}

		@Test
		void asPathWithAbsolutePath() {
			// Given
			CommandArguments sut = CommandArguments.of("/absolute/path/file.txt");
			State state = Mockito.mock(State.class);
			Path cwd = Paths.get("/home/user");
			when(state.getCwd()).thenReturn(cwd);

			// When
			Path result = sut.get(0).asPath(state);

			// Then
			assertThat(result).isAbsolute();
			// the runner on github is producing d:/absolute/path/file.txt
			assertThat(result.toString()).endsWith("/absolute/path/file.txt");
		}

	}
}
