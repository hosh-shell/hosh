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
package org.hosh.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hosh.testsupport.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Optional;

import org.hosh.modules.TextModule.Count;
import org.hosh.modules.TextModule.Distinct;
import org.hosh.modules.TextModule.Drop;
import org.hosh.modules.TextModule.Duplicated;
import org.hosh.modules.TextModule.Enumerate;
import org.hosh.modules.TextModule.Filter;
import org.hosh.modules.TextModule.Regex;
import org.hosh.modules.TextModule.Schema;
import org.hosh.modules.TextModule.Sort;
import org.hosh.modules.TextModule.Table;
import org.hosh.modules.TextModule.Take;
import org.hosh.modules.TextModuleTest.CountTest;
import org.hosh.modules.TextModuleTest.DistinctTest;
import org.hosh.modules.TextModuleTest.DropTest;
import org.hosh.modules.TextModuleTest.DuplicatedTest;
import org.hosh.modules.TextModuleTest.EnumerateTest;
import org.hosh.modules.TextModuleTest.FilterTest;
import org.hosh.modules.TextModuleTest.RegexTest;
import org.hosh.modules.TextModuleTest.SchemaTest;
import org.hosh.modules.TextModuleTest.SortTest;
import org.hosh.modules.TextModuleTest.TableTest;
import org.hosh.modules.TextModuleTest.TakeTest;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({
		RegexTest.class,
		SchemaTest.class,
		CountTest.class,
		EnumerateTest.class,
		DropTest.class,
		TakeTest.class,
		FilterTest.class,
		SortTest.class,
		DistinctTest.class,
		DuplicatedTest.class,
		TableTest.class,
})
public class TextModuleTest {

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class RegexTest {

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
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 2 parameters")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(Arrays.asList("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 2 parameters")));
		}

		@Test
		public void twoArgsNoInput() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsNonMatching() {
			Record record = Record.of(Keys.TEXT, Values.ofText(""));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(Keys.TEXT.name(), "(?<id>\\\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMatching() {
			Record record = Record.of(Keys.TEXT, Values.ofText("1"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(Keys.TEXT.name(), "(?<id>\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Record.of(Keys.of("id"), Values.ofText("1")));
			then(err).shouldHaveZeroInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoArgsMissingKey() {
			Record record = Record.of(Keys.TEXT, Values.ofText("1"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(Keys.COUNT.name(), "(?<id>\\d+)"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SchemaTest {

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
			Record record = Record.of(Keys.COUNT, null).append(Keys.INDEX, null);
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).should().send(Record.of(Keys.of("schema"), Values.ofText("count index")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(Arrays.asList("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class CountTest {

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
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(out).should().send(Record.of(Keys.COUNT, Values.ofNumeric(2)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneRecord() {
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(2)).recv();
			then(out).should().send(Record.of(Keys.COUNT, Values.ofNumeric(1)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroRecords() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(1)).recv();
			then(out).should().send(Record.of(Keys.COUNT, Values.ofNumeric(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class EnumerateTest {

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
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(Record.empty()), Optional.empty());
			sut.run(Arrays.asList(), in, out, err);
			then(out).should().send(Record.builder().entry(Keys.INDEX, Values.ofNumeric(1)).entry(Keys.TEXT, Values.ofText("some data")).build());
			then(out).should().send(Record.of(Keys.INDEX, Values.ofNumeric(2)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class DropTest {

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
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			sut.run(Arrays.asList("0"), in, out, err);
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void dropOne() {
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			sut.run(Arrays.asList("1"), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroArgs() {
			sut.run(Arrays.asList(), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class TakeTest {

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
			ExitStatus exitStatus = sut.run(Arrays.asList("0"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeExactly() {
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("1"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(1)).recv();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeLess() {
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("1"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(1)).recv();
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void takeMore() {
			Record record = Record.of(Keys.TEXT, Values.ofText("some data"));
			Record record2 = Record.of(Keys.TEXT, Values.ofText("another value"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("5"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(out).should().send(record);
			then(out).should().send(record2);
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isError();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class FilterTest {

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
			Record record = Record.of(Keys.TEXT, Values.ofText("some string"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			sut.run(Arrays.asList(Keys.TEXT.name(), ".*string.*"), in, out, err);
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void ignoreNonMatchingLines() {
			Record record = Record.of(Keys.TEXT, Values.ofText("some string"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			sut.run(Arrays.asList("key", ".*number.*"), in, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroArgs() {
			sut.run(Arrays.asList(), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 2 parameters: key regex")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("key"), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 2 parameters: key regex")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SortTest {

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
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Record.of(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record2, record1);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void sortByNonExistingKey() {
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(2));
			given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("anotherkey"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(2)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(1)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isError();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class DistinctTest {

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
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Record.of(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("anotherkey"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(0)).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void distinctValues() {
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Record.of(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1, record2);
		}

		@SuppressWarnings("unchecked")
		@Test
		public void duplicatedValues() {
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Record.of(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(1)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isError();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class DuplicatedTest {

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
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Record.of(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("anotherkey"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(0)).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void distinctValues() {
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(2));
			Record record2 = Record.of(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(0)).send(records.capture());
			assertThat(records.getAllValues()).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void duplicatedValues() {
			Record record1 = Record.of(Keys.NAME, Values.ofNumeric(1));
			Record record2 = Record.of(Keys.NAME, Values.ofNumeric(1));
			given(in.recv()).willReturn(Optional.of(record1), Optional.of(record2), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList("name"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(3)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(1)).send(records.capture());
			assertThat(records.getAllValues()).containsExactlyInAnyOrder(record1);
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isError();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class TableTest {

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
			Record record1 = Record.builder().entry(Keys.COUNT, Values.ofNumeric(2)).entry(Keys.TEXT, Values.ofText("zvrnv")).build();
			given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(2)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(
					Record.of(Keys.of("header"), Values.ofText("count     text      ")),
					Record.of(Keys.of("row"), Values.ofText("2         zvrnv     ")));
		}

		@SuppressWarnings("unchecked")
		@Test
		public void tableWithNone() {
			Record record1 = Record.builder().entry(Keys.COUNT, Values.none()).entry(Keys.TEXT, Values.ofText("zvrnv")).build();
			given(in.recv()).willReturn(Optional.of(record1), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(2)).recv();
			then(err).shouldHaveNoMoreInteractions();
			verify(out, Mockito.times(2)).send(records.capture());
			assertThat(records.getAllValues()).containsExactly(
					Record.of(Keys.of("header"), Values.ofText("count     text      ")),
					Record.of(Keys.of("row"), Values.ofText("          zvrnv     ")));
		}

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList("a"), in, out, err);
			assertThat(exitStatus).isError();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}
}
