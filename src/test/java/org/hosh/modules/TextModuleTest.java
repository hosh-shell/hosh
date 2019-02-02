package org.hosh.modules;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.Optional;

import org.hosh.modules.TextModule.Drop;
import org.hosh.modules.TextModule.Enumerate;
import org.hosh.modules.TextModule.Schema;
import org.hosh.modules.TextModuleTest.DropTest;
import org.hosh.modules.TextModuleTest.EnumerateTest;
import org.hosh.modules.TextModuleTest.SchemaTest;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({
		SchemaTest.class,
		EnumerateTest.class,
		DropTest.class
})
public class TextModuleTest {
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
			Record record = Record.of("key1", null).append("key2", null);
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			sut.run(Arrays.asList(), in, out, err);
			then(out).should().send(Record.of("keys", Values.ofText("[key1, key2]")));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expected 0 parameters")));
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
			Record record = Record.of("key", Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.of(Record.empty()), Optional.empty());
			sut.run(Arrays.asList(), in, out, err);
			then(out).should().send(Record.of("index", Values.ofNumeric(1)).append("key", Values.ofText("some data")));
			then(out).should().send(Record.of("index", Values.ofNumeric(2)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expected 0 parameters")));
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
			Record record = Record.of("key", Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			sut.run(Arrays.asList("0"), in, out, err);
			then(out).should().send(record);
			then(err).shouldHaveNoMoreInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void dropOne() {
			Record record = Record.of("key", Values.ofText("some data"));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			sut.run(Arrays.asList("1"), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void zeroArgs() {
			sut.run(Arrays.asList(), in, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expected 1 parameter")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}
}
