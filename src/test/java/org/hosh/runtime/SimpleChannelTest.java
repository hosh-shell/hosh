package org.hosh.runtime;

import static org.mockito.BDDMockito.then;

import java.io.PrintStream;

import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SimpleChannelTest {
	@Mock
	private PrintStream printStream;
	private SimpleChannel sut;

	@Before
	public void setup() {
		sut = new SimpleChannel(printStream);
	}

	@Test
	public void empty() {
		sut.send(Record.empty());
		then(printStream).should().println();
	}

	@Test
	public void oneValue() {
		sut.send(Record.of("key", Values.ofText("foo")));
		then(printStream).should().append("foo");
		then(printStream).should().println();
	}

	@Test
	public void twoValues() {
		sut.send(Record.of("key", Values.ofText("foo"), "another_key", Values.ofText("bar")));
		then(printStream).should().append("foo");
		then(printStream).should().append(" ");
		then(printStream).should().append("bar");
		then(printStream).should().println();
	}
}
