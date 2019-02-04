package org.hosh.runtime;

import static org.mockito.BDDMockito.then;

import java.io.IOException;

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
	private Appendable appendable;
	private SimpleChannel sut;

	@Before
	public void setup() {
		sut = new SimpleChannel(appendable);
	}

	@Test
	public void empty() throws IOException {
		sut.send(Record.empty());
		then(appendable).should().append(System.lineSeparator());
	}

	@Test
	public void oneValue() throws IOException {
		sut.send(Record.of("key", Values.ofText("foo")));
		then(appendable).should().append("foo");
		then(appendable).should().append(System.lineSeparator());
	}

	@Test
	public void twoValues() throws IOException {
		sut.send(Record.of("key", Values.ofText("foo"), "another_key", Values.ofText("bar")));
		then(appendable).should().append("foo");
		then(appendable).should().append(" ");
		then(appendable).should().append("bar");
		then(appendable).should().append(System.lineSeparator());
	}
}
