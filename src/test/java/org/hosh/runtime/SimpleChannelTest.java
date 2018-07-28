package org.hosh.runtime;

import static org.mockito.BDDMockito.then;

import java.io.PrintStream;

import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SimpleChannelTest {
	@Mock
	private PrintStream printStream;
	@InjectMocks
	private SimpleChannel sut;

	@Test
	public void empty() {
		sut.send(Record.empty());
		then(printStream).should().println("");
	}

	@Test
	public void oneValue() {
		sut.send(Record.of("key", Values.ofText("foo")));
		Mockito.verify(printStream).println("foo");
	}

	@Test
	public void twoValues() {
		sut.send(Record.of("key", Values.ofText("foo"), "another_key", Values.ofText("bar")));
		Mockito.verify(printStream).println("foo bar");
	}
}
