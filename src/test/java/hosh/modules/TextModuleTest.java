/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package hosh.modules;

import static hosh.testsupport.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import hosh.modules.TextModule.Count;
import hosh.modules.TextModule.Distinct;
import hosh.modules.TextModule.Drop;
import hosh.modules.TextModule.Duplicated;
import hosh.modules.TextModule.Enumerate;
import hosh.modules.TextModule.Filter;
import hosh.modules.TextModule.Regex;
import hosh.modules.TextModule.Schema;
import hosh.modules.TextModule.Select;
import hosh.modules.TextModule.Sort;
import hosh.modules.TextModule.Split;
import hosh.modules.TextModule.Table;
import hosh.modules.TextModule.Take;
import hosh.modules.TextModule.Timestamp;
import hosh.modules.TextModule.Trim;
import hosh.spi.Channel;
import hosh.spi.ExitStatus;
import hosh.spi.Keys;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;

public class TextModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TrimTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Trim sut;

		@Test
		public void empty() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("text"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 argument")));
		}

		@SuppressWarnings("unchecked")
		@Test
		public void trimMissingKey() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("  abc  "));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.ERROR.name()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void trimKey() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("  abc  "));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("abc")));
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void trimKeyOfDifferentType() {
			Record record = Records.singleton(Keys.COUNT, Values.ofNumeric(42));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.COUNT.name()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SelectTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Select sut;

		@Test
		public void empty() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.COUNT.name()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void noArgs() {
			given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.NAME, Values.ofNumeric(1))), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.empty());
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneArgKeepKey() {
			Record record = Records.singleton(Keys.NAME, Values.ofText("foo"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.NAME.name()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(record);
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsIgnoreMissingKeys() {
			Record record = Records.singleton(Keys.NAME, Values.ofText("foo"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.NAME.name(), Keys.COUNT.name()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(record);
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SplitTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Split sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
		}

		@Test
		public void twoArgsNoInput() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), " "), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMatching() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("a b c"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), " "), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.of("1"), Values.ofText("a"))
					.entry(Keys.of("2"), Values.ofText("b"))
					.entry(Keys.of("3"), Values.ofText("c"))
					.build());
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class RegexTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Regex sut;

		@Test
		public void zeroArg() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments")));
		}

		@Test
		public void twoArgsNoInput() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsNonMatching() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText(""));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMatching() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("1"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), "(?<id>\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.singleton(Keys.of("id"), Values.ofText("1")));
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMissingKey() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("1"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.COUNT.name(), "(?<id>\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SchemaTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Schema sut;

		@SuppressWarnings("unchecked")
		@Test
		public void zeroArg() {
			Record record = Records.singleton(Keys.COUNT, null).append(Keys.INDEX, null);
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.of("schema"), Values.ofText("count index")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class CountTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Count sut;

		@SuppressWarnings("unchecked")
		@Test
		public void twoRecords() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(2)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneRecord() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(1)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroRecords() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class EnumerateTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Enumerate sut;

		@SuppressWarnings("unchecked")
		@Test
		public void zeroArg() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(Records.empty()), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Records.builder().entry(Keys.INDEX, Values.ofNumeric(1)).entry(Keys.TEXT, Values.ofText("some data")).build());
			then(out).should().send(Records.singleton(Keys.INDEX, Values.ofNumeric(2)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TimestampTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@Mock
		private Clock clock;

		@InjectMocks
		private Timestamp sut;

		@SuppressWarnings("unchecked")
		@Test
		public void zeroArg() {
			given(clock.instant()).willReturn(Instant.EPOCH);
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(Records.empty()), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(
					Records.builder()
							.entry(Keys.TIMESTAMP, Values.ofInstant(Instant.EPOCH))
							.entry(Keys.TEXT, Values.ofText("some data"))
							.build());
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DropTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Drop sut;

		@SuppressWarnings("unchecked")
		@Test
		public void dropZero() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("0"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void dropOne() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void negativeArg() {
			ExitStatus exitStatus = sut.run(List.of("-1"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TakeTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Take sut;

		@Test
		public void takeZero() {
			ExitStatus exitStatus = sut.run(List.of("0"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeExactly() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeLess() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeMore() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			Record record2 = Records.singleton(Keys.TEXT, Values.ofText("another value"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("5"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(out).should().send(record2);
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void negativeArg() {
			ExitStatus exitStatus = sut.run(List.of("-1"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(out).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class FilterTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Filter sut;

		@SuppressWarnings("unchecked")
		@Test
		public void printMatchingLines() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some string"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), ".*string.*"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void ignoreNonMatchingLines() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some string"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("key", ".*number.*"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments: key regex")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("key"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments: key regex")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SortTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Sort sut;

		@Captor
		private ArgumentCaptor<Record> records;

		@SuppressWarnings("unchecked")
		@Test
		public void sortByExistingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void sortByNonExistingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("anotherkey"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.times(1)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DistinctTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Distinct sut;

		@Captor
		private ArgumentCaptor<Record> records;

		@SuppressWarnings("unchecked")
		@Test
		public void missingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("anotherkey"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.never()).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void distinctValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1, record2);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void duplicatedValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should().send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DuplicatedTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Duplicated sut;

		@Captor
		private ArgumentCaptor<Record> records;

		@SuppressWarnings("unchecked")
		@Test
		public void missingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("anotherkey"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.times(0)).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void distinctValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.never()).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void duplicatedValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.times(1)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TableTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Table sut;

		@Captor
		private ArgumentCaptor<Record> records;

		@SuppressWarnings("unchecked")
		@Test
		public void table() {
			Record record1 = Records.builder().entry(Keys.COUNT, Values.ofNumeric(2)).entry(Keys.TEXT, Values.ofText("zvrnv")).build();
			given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(
					Records.singleton(Keys.of("header"), Values.ofText("count     text      ")),
					Records.singleton(Keys.of("row"), Values.ofText("2         zvrnv     ")));
		}

		@SuppressWarnings("unchecked")
		@Test
		public void tableWithNone() {
			Record record1 = Records.builder().entry(Keys.COUNT, Values.none()).entry(Keys.TEXT, Values.ofText("zvrnv")).build();
			given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).should(Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(
					Records.singleton(Keys.of("header"), Values.ofText("count     text      ")),
					Records.singleton(Keys.of("row"), Values.ofText("          zvrnv     ")));
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of("a"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}
}
