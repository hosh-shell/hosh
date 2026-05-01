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
package hosh.modules.text;

import hosh.modules.text.TextModule.Count;
import hosh.modules.text.TextModule.Distinct;
import hosh.modules.text.TextModule.Drop;
import hosh.modules.text.TextModule.Duplicated;
import hosh.modules.text.TextModule.Enumerate;
import hosh.modules.text.TextModule.Filter;
import hosh.modules.text.TextModule.Join;
import hosh.modules.text.TextModule.Regex;
import hosh.modules.text.TextModule.Schema;
import hosh.modules.text.TextModule.Select;
import hosh.modules.text.TextModule.Sort;
import hosh.modules.text.TextModule.Split;
import hosh.modules.text.TextModule.Sum;
import hosh.modules.text.TextModule.Take;
import hosh.modules.text.TextModule.Timestamp;
import hosh.modules.text.TextModule.Trim;
import hosh.spi.test.support.RecordMatcher;
import hosh.spi.CommandArguments;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import hosh.test.support.WithThread;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

class TextModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class RandTest {

		@RegisterExtension
		final WithThread withThread = new WithThread();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		TextModule.Rand sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Rand();
		}

		// not a very good test, just checking if rand can be interrupted
		@Test
		void interrupt() {
			// Given
			withThread.interrupt();
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("asd"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: rand")));
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class TrimTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Trim sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Trim();
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: trim key")));
		}

		@SuppressWarnings("unchecked")
		@Test
		void trimMissingKey() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("  abc  "));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.ERROR.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void trimKey() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("  abc  "));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("abc")));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void trimKeyOfDifferentType() {
			// Given
			Record record = Records.singleton(Keys.COUNT, Values.ofInstant(Instant.EPOCH));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.COUNT.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class JoinTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Join sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Join();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: join separator")));
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(","), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void oneValue() {
			// Given
			Record record = Records.singleton(Keys.INDEX, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(","), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("1")));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void twoValues() {
			// Given
			Record record = Records.builder().entry(Keys.INDEX, Values.ofNumeric(1)).entry(Keys.NAME, Values.ofNumeric(2)).build();
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(","), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("1,2")));
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SumTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Sum sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Sum();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sum key")));
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.SIZE, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void nonMatchingKey() {
			// Given
			Record record = Records.singleton(Keys.INDEX, Values.ofSize(1));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.SIZE, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyWithSizeValue() {
			// Given
			Record record = Records.singleton(Keys.SIZE, Values.ofSize(1));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.SIZE, Values.ofSize(2)));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyWithNumericValue() {
			// Given
			Record record = Records.singleton(Keys.COUNT, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("count"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.COUNT, Values.ofNumeric(2)));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyWithSizeValueAndNone() {
			// Given
			Record record1 = Records.singleton(Keys.SIZE, Values.none());
			Record record2 = Records.singleton(Keys.SIZE, Values.ofSize(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.SIZE, Values.ofSize(1)));
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class FreqTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		TextModule.Freq sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Freq();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: freq key")));
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void nonMatchingKey() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyOneRecord() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.VALUE, Values.ofText("aaa"), Keys.COUNT, Values.ofNumeric(1)));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyTwoRecords() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.VALUE, Values.ofText("aaa"), Keys.COUNT, Values.ofNumeric(2)));
			then(err).shouldHaveNoInteractions();
		}

		// an important corner case: counting "none" as any other value
		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyWithNone() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.none());
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(Keys.VALUE, Values.none(), Keys.COUNT, Values.ofNumeric(1)));
			then(err).shouldHaveNoInteractions();
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class MinTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		TextModule.Min sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Min();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: min key")));
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Min.MIN, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void nonMatchingKey() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Min.MIN, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyOneRecord() {
			// Given
			Record record = Records.singleton(Keys.INDEX, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.INDEX.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Min.MIN, Values.ofNumeric(1)));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyTwoRecords() {
			// Given
			Record record1 = Records.singleton(Keys.INDEX, Values.ofNumeric(10));
			Record record2 = Records.singleton(Keys.INDEX, Values.ofNumeric(-10));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.INDEX.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Min.MIN, Values.ofNumeric(-10)));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyTwoRecordsWithNone() {
			// Given
			Record record1 = Records.singleton(Keys.INDEX, Values.ofNumeric(10));
			Record record2 = Records.singleton(Keys.INDEX, Values.none());
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.INDEX.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Min.MIN, Values.ofNumeric(10)));
			then(err).shouldHaveNoInteractions();
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class MaxTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		TextModule.Max sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Max();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: max key")));
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Max.MAX, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void nonMatchingKey() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Max.MAX, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyOneRecord() {
			// Given
			Record record = Records.singleton(Keys.INDEX, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.INDEX.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Max.MAX, Values.ofNumeric(1)));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyTwoRecords() {
			// Given
			Record record1 = Records.singleton(Keys.INDEX, Values.ofNumeric(10));
			Record record2 = Records.singleton(Keys.INDEX, Values.ofNumeric(-10));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.INDEX.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Max.MAX, Values.ofNumeric(10)));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchingKeyTwoRecordsWithNone() {
			// Given
			Record record1 = Records.singleton(Keys.INDEX, Values.ofNumeric(10));
			Record record2 = Records.singleton(Keys.INDEX, Values.none());
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.INDEX.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(RecordMatcher.of(TextModule.Max.MAX, Values.ofNumeric(10)));
			then(err).shouldHaveNoInteractions();
		}

	}


	@Nested
	@ExtendWith(MockitoExtension.class)
	class SelectTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Select sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Select();
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.COUNT.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void noArgs() {
			// Given
			given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.NAME, Values.ofNumeric(1))), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.empty());
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void oneArgKeepKey() {
			// Given
			Record record = Records.singleton(Keys.NAME, Values.ofText("foo"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.NAME.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).should(times(2)).recv();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void twoArgsIgnoreMissingKeys() {
			// Given
			Record record = Records.singleton(Keys.NAME, Values.ofText("foo"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.NAME.name(), Keys.COUNT.name()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).should(times(2)).recv();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SplitTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Split sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Split();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
		}

		@Test
		void twoArgsNoInput() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name(), " "), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void twoArgsMatching() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("a b c"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name(), " "), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).should(times(2)).recv();
			then(out).should().send(Records.builder()
					.entry(Keys.of("1"), Values.ofText("a"))
					.entry(Keys.of("2"), Values.ofText("b"))
					.entry(Keys.of("3"), Values.ofText("c"))
					.build());
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class RegexTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Regex sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Regex();
		}

		@Test
		void zeroArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: regex key regex")));
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("asd"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: regex key regex")));
		}

		@Test
		void twoArgsNoInput() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void twoArgsNonMatching() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText(""));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void twoArgsMatching() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("1"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name(), "(?<id>\\d+)"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).should(times(2)).recv();
			then(out).should().send(Records.singleton(Keys.of("id"), Values.ofText("1")));
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void twoArgsMissingKey() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("1"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.COUNT.name(), "(?<id>\\d+)"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).should(times(2)).recv();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SchemaTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Schema sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Schema();
		}

		@SuppressWarnings("unchecked")
		@Test
		void zeroArg() {
			// Given
			Record record = Records.singleton(Keys.COUNT, Values.none()).append(Keys.INDEX, Values.none());
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.of("schema"), Values.ofText("count index")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("asd"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: schema")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class CountTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Count sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Count();
		}

		@SuppressWarnings("unchecked")
		@Test
		void twoRecords() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(2)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void oneRecord() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(1)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void zeroRecords() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("asd"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: count")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class EnumerateTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Enumerate sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Enumerate();
		}

		@SuppressWarnings("unchecked")
		@Test
		void zeroArg() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(Records.empty()), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.builder().entry(Keys.INDEX, Values.ofNumeric(1)).entry(Keys.TEXT, Values.ofText("some data")).build());
			then(out).should().send(Records.singleton(Keys.INDEX, Values.ofNumeric(2)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("asd"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: enumerate")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class TimestampTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		Clock clock;

		Timestamp sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Timestamp();
			sut.setClock(clock);
		}

		@SuppressWarnings("unchecked")
		@Test
		void zeroArg() {
			// Given
			given(clock.instant()).willReturn(Instant.EPOCH);
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(Records.empty()), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(
					Records.builder()
							.entry(Keys.TIMESTAMP, Values.ofInstant(Instant.EPOCH))
							.entry(Keys.TEXT, Values.ofText("some data"))
							.build());
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("asd"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: timestamp")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class DropTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Drop sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Drop();
		}

		@SuppressWarnings("unchecked")
		@Test
		void dropZero() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("0"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void dropOne() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("1"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: drop number")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void negativeArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("-1"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("number must be >= 0")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class TakeTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Take sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Take();
		}

		@Test
		void takeZero() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("0"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).should().recv();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void takeExactly() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("1"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void takeLess() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("1"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void takeMore() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			Record record2 = Records.singleton(Keys.TEXT, Values.ofText("another value"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("5"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(out).should().send(record2);
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void negativeArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("-1"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("number must be >= 0")));
		}

		@Test
		void noArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(out).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: take number")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class LastTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		TextModule.Last sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Last();
		}

		@Test
		void lastNoArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: last number")));
		}

		@Test
		void lastOneInvalidArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("0"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("number must be >= 1")));
		}

		@SuppressWarnings("unchecked")
		@Test
		void lastOneWithOneRecord() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("1"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void lastOneWithMultipleRecords() {
			// Given
			Record data1 = Records.singleton(Keys.TEXT, Values.ofText("data 1"));
			Record data2 = Records.singleton(Keys.TEXT, Values.ofText("data 2"));
			given(in.recv()).willReturn(Optional.of(data1), Optional.of(data2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("1"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(data2);
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void lastTwoWithOneRecord() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("2"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class FilterTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		Filter sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Filter();
		}

		@SuppressWarnings("unchecked")
		@Test
		void printMatchingLines() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some string"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(Keys.TEXT.name(), ".*string.*"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void ignoreNonMatchingLines() {
			// Given
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some string"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("key", ".*number.*"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void zeroArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: filter key regex")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("key"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: filter key regex")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SortTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Captor
		ArgumentCaptor<Record> records;

		Sort sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Sort();
		}

		@Test
		void empty() {
			// Given
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void sortByExistingKey() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		void sortByExistingKeyNullLast() {
			// Given
			Record record1 = Records.singleton(Keys.SIZE, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record3 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.of(record3), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(3)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(record3, record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		void sortByNonExistingKey() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("size"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(1)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		void sortAscByExistingKey() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name", "asc"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		void sortDescByExistingKey() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name", "desc"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(record1, record2);
		}

		@Test
		void invalidDirection() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("ZZZ", "name"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("must be 'asc' or 'desc'")));
			then(out).shouldHaveNoMoreInteractions();
		}

		@Test
		void zeroArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sort key [asc|desc]")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("asc", "key", "aaa"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sort key [asc|desc]")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class DistinctTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Captor
		ArgumentCaptor<Record> records;

		Distinct sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Distinct();
		}

		@SuppressWarnings("unchecked")
		@Test
		void missingKey() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("anotherkey"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.never()).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		void distinctValues() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1, record2);
		}

		@SuppressWarnings("unchecked")
		@Test
		void duplicatedValues() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should().send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		void zeroArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: distinct key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class DuplicatedTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Captor
		ArgumentCaptor<Record> records;

		Duplicated sut;

		@BeforeEach
		void createSut() {
			sut = new TextModule.Duplicated();
		}

		@SuppressWarnings("unchecked")
		@Test
		void missingKey() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("anotherkey"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(0)).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		void distinctValues() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.never()).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		void duplicatedValues() {
			// Given
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("name"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(times(1)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		void zeroArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: duplicated key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	class SortPropertyTest {

		@Property
		void sortIsIdempotent(@ForAll("textRecordLists") List<Record> input) {
			Sort sut = new Sort();
			List<Record> firstSort = runSort(sut, input);
			List<Record> secondSort = runSort(sut, firstSort);
			assertThat(firstSort).containsExactlyElementsOf(secondSort);
		}

		private List<Record> runSort(Sort sut, List<Record> input) {
			List<Record> result = new ArrayList<>();
			sut.run(CommandArguments.of("name"), fromList(input), result::add, record -> {});
			return result;
		}
	}

	@Nested
	class TakeDropPropertyTest {

		@Property
		void takeAndDropPartitionInput(@ForAll("textRecordLists") List<Record> input,
				@ForAll @IntRange(min = 0, max = 20) int n) {
			Assume.that(n <= input.size());
			Take takeSut = new Take();
			Drop dropSut = new Drop();
			OutputChannel noopErr = record -> {};

			List<Record> taken = new ArrayList<>();
			takeSut.run(CommandArguments.of(String.valueOf(n)), fromList(input), taken::add, noopErr);

			List<Record> dropped = new ArrayList<>();
			dropSut.run(CommandArguments.of(String.valueOf(n)), fromList(input), dropped::add, noopErr);

			List<Record> combined = new ArrayList<>(taken);
			combined.addAll(dropped);
			assertThat(combined).containsExactlyElementsOf(input);
		}
	}

	@Provide
	Arbitrary<List<Record>> textRecordLists() {
		return Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(10)
				.map(s -> Records.singleton(Keys.NAME, Values.ofText(s)))
				.list().ofMinSize(0).ofMaxSize(20);
	}

	private static InputChannel fromList(List<Record> records) {
		Iterator<Record> it = records.iterator();
		return () -> it.hasNext() ? Optional.of(it.next()) : Optional.empty();
	}

}
