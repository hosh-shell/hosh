package hosh.modules.text;/*
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

import hosh.modules.TextModule.Count;
import hosh.modules.TextModule.Distinct;
import hosh.modules.TextModule.Drop;
import hosh.modules.TextModule.Duplicated;
import hosh.modules.TextModule.Enumerate;
import hosh.modules.TextModule.Filter;
import hosh.modules.TextModule.Join;
import hosh.modules.TextModule.Regex;
import hosh.modules.TextModule.Schema;
import hosh.modules.TextModule.Select;
import hosh.modules.TextModule.Sort;
import hosh.modules.TextModule.Split;
import hosh.modules.TextModule.Sum;
import hosh.modules.TextModule.Table;
import hosh.modules.TextModule.Take;
import hosh.modules.TextModule.Timestamp;
import hosh.modules.TextModule.Trim;
import hosh.modules.text.TextModule;
import hosh.spi.Ansi;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import hosh.testsupport.RecordMatcher;
import hosh.testsupport.WithThread;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

public class TextModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class RandTest {

		@RegisterExtension
		public final WithThread withThread = new WithThread();

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private TextModule.Rand sut;

		@Test // not a very good test, just checking if rand can be interrupted
		public void interrupt() {
			withThread.interrupt();
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TrimTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Trim sut;

		@Test
		public void empty() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("text"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 argument")));
		}

		@SuppressWarnings("unchecked")
		@Test
		public void trimMissingKey() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("  abc  "));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.ERROR.name()), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void trimKey() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("  abc  "));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name()), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("abc")));
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void trimKeyOfDifferentType() {
			Record record = Records.singleton(Keys.COUNT, Values.ofInstant(Instant.EPOCH));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.COUNT.name()), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class JoinTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Join sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: join separator")));
		}

		@Test
		public void empty() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(","), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneValue() {
			Record record = Records.singleton(Keys.INDEX, Values.ofNumeric(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(","), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("1")));
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoValues() {
			Record record = Records.builder().entry(Keys.INDEX, Values.ofNumeric(1)).entry(Keys.NAME, Values.ofNumeric(2)).build();
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(","), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("1,2")));
			BDDMockito.then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SumTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Sum sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sum key")));
		}

		@Test
		public void empty() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("size"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(RecordMatcher.of(Keys.SIZE, Values.ofSize(0)));
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void nonMatchingKey() {
			Record record = Records.singleton(Keys.INDEX, Values.ofSize(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("size"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(RecordMatcher.of(Keys.SIZE, Values.ofSize(0)));
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void matchingKey() {
			Record record = Records.singleton(Keys.SIZE, Values.ofSize(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("size"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(RecordMatcher.of(Keys.SIZE, Values.ofSize(2)));
			BDDMockito.then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SelectTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Select sut;

		@Test
		public void empty() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.COUNT.name()), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void noArgs() {
			BDDMockito.given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.NAME, Values.ofNumeric(1))), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.empty());
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneArgKeepKey() {
			Record record = Records.singleton(Keys.NAME, Values.ofText("foo"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.NAME.name()), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).should(times(2)).recv();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsIgnoreMissingKeys() {
			Record record = Records.singleton(Keys.NAME, Values.ofText("foo"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.NAME.name(), Keys.COUNT.name()), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).should(times(2)).recv();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SplitTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Split sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name()), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
		}

		@Test
		public void twoArgsNoInput() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), " "), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMatching() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("a b c"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), " "), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).should(times(2)).recv();
			BDDMockito.then(out).should().send(Records.builder()
					.entry(Keys.of("1"), Values.ofText("a"))
					.entry(Keys.of("2"), Values.ofText("b"))
					.entry(Keys.of("3"), Values.ofText("c"))
					.build());
			BDDMockito.then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class RegexTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Regex sut;

		@Test
		public void zeroArg() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments")));
		}

		@Test
		public void twoArgsNoInput() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsNonMatching() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText(""));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMatching() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("1"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), "(?<id>\\d+)"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).should(times(2)).recv();
			BDDMockito.then(out).should().send(Records.singleton(Keys.of("id"), Values.ofText("1")));
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMissingKey() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("1"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.COUNT.name(), "(?<id>\\d+)"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).should(times(2)).recv();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SchemaTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Schema sut;

		@SuppressWarnings("unchecked")
		@Test
		public void zeroArg() {
			Record record = Records.singleton(Keys.COUNT, null).append(Keys.INDEX, null);
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.singleton(Keys.of("schema"), Values.ofText("count index")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class CountTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Count sut;

		@SuppressWarnings("unchecked")
		@Test
		public void twoRecords() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(2)));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneRecord() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(1)));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroRecords() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.singleton(Keys.COUNT, Values.ofNumeric(0)));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class EnumerateTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Enumerate sut;

		@SuppressWarnings("unchecked")
		@Test
		public void zeroArg() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.of(Records.empty()), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(Records.builder().entry(Keys.INDEX, Values.ofNumeric(1)).entry(Keys.TEXT, Values.ofText("some data")).build());
			BDDMockito.then(out).should().send(Records.singleton(Keys.INDEX, Values.ofNumeric(2)));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TimestampTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Mock
		private Clock clock;

		@InjectMocks
		private Timestamp sut;

		@SuppressWarnings("unchecked")
		@Test
		public void zeroArg() {
			BDDMockito.given(clock.instant()).willReturn(Instant.EPOCH);
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.of(Records.empty()), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(
					Records.builder()
							.entry(Keys.TIMESTAMP, Values.ofInstant(Instant.EPOCH))
							.entry(Keys.TEXT, Values.ofText("some data"))
							.build());
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DropTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Drop sut;

		@SuppressWarnings("unchecked")
		@Test
		public void dropZero() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("0"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void dropOne() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void negativeArg() {
			ExitStatus exitStatus = sut.run(List.of("-1"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TakeTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Take sut;

		@Test
		public void takeZero() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("0"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).should().recv();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeExactly() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeLess() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeMore() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some data"));
			Record record2 = Records.singleton(Keys.TEXT, Values.ofText("another value"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("5"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(out).should().send(record2);
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoInteractions();
		}

		@Test
		public void negativeArg() {
			ExitStatus exitStatus = sut.run(List.of("-1"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class FilterTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Filter sut;

		@SuppressWarnings("unchecked")
		@Test
		public void printMatchingLines() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some string"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(Keys.TEXT.name(), ".*string.*"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(record);
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void ignoreNonMatchingLines() {
			Record record = Records.singleton(Keys.TEXT, Values.ofText("some string"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("key", ".*number.*"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments: key regex")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("key"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments: key regex")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SortTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Captor
		private ArgumentCaptor<Record> records;

		@InjectMocks
		private Sort sut;

		@Test
		public void empty() {
			BDDMockito.given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void sortByExistingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(2)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactly(record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void sortByExistingKeyNullLast() {
			Record record1 = Records.singleton(Keys.SIZE, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record3 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.of(record3), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(3)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactly(record3, record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void sortByNonExistingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("size"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(1)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactly(record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void sortAscByExistingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("asc", "name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(2)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactly(record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void sortDescByExistingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofText("bbb"));
			Record record2 = Records.singleton(Keys.NAME, Values.ofText("aaa"));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("desc", "name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(2)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactly(record1, record2);
		}

		@Test
		public void invalidDirection() {
			ExitStatus exitStatus = sut.run(List.of("ZZZ", "name"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("must be asc or desc")));
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("use 'sort key' or 'sort [asc|desc] key'")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void tooManyArgs() {
			ExitStatus exitStatus = sut.run(List.of("asc", "key", "aaa"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("too many args")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DistinctTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Distinct sut;

		@Captor
		private ArgumentCaptor<Record> records;

		@SuppressWarnings("unchecked")
		@Test
		public void missingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("anotherkey"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(Mockito.never()).send(records.capture());
			Assertions.assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void distinctValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(2)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1, record2);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void duplicatedValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should().send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DuplicatedTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Duplicated sut;

		@Captor
		private ArgumentCaptor<Record> records;

		@SuppressWarnings("unchecked")
		@Test
		public void missingKey() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("anotherkey"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(0)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void distinctValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(Mockito.never()).send(records.capture());
			Assertions.assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void duplicatedValues() {
			Record record1 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Records.singleton(Keys.NAME, Values.ofNumeric(1));
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("name"), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(1)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class TableTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Table sut;

		@Captor
		private ArgumentCaptor<Record> records;

		@SuppressWarnings("unchecked")
		@Test
		public void table() {
			Record record1 = Records.builder().entry(Keys.COUNT, Values.ofNumeric(2)).entry(Keys.TEXT, Values.ofText("whatever")).build();
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(2)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactly(
					Records.singleton(Keys.of("header"), Values.withStyle(Values.ofText("count     text      "), Ansi.Style.FG_CYAN)),
					Records.singleton(Keys.of("row"), Values.withStyle(Values.ofText("2         whatever  "), Ansi.Style.BG_BLUE)));
		}

		@SuppressWarnings("unchecked")
		@Test
		public void tableWithNone() {
			Record record1 = Records.builder().entry(Keys.COUNT, Values.none()).entry(Keys.TEXT, Values.ofText("whatever")).build();
			BDDMockito.given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			Assertions.assertThat(exitStatus).isSuccess();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).should(times(2)).send(records.capture());
			Assertions.assertThat(records.getAllValues()).containsExactly(
					Records.singleton(Keys.of("header"), Values.withStyle(Values.ofText("count     text      "), Ansi.Style.FG_CYAN)),
					Records.singleton(Keys.of("row"), Values.withStyle(Values.ofText("          whatever  "), Ansi.Style.BG_BLUE)));
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of("a"), in, out, err);
			Assertions.assertThat(exitStatus).isError();
			BDDMockito.then(in).shouldHaveNoMoreInteractions();
			BDDMockito.then(out).shouldHaveNoMoreInteractions();
			BDDMockito.then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
			BDDMockito.then(err).shouldHaveNoMoreInteractions();
		}
	}
}
