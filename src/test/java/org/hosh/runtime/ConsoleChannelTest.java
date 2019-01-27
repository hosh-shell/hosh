package org.hosh.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.PrintWriter;

import org.hosh.runtime.ConsoleChannel.Color;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ConsoleChannelTest {
	@Mock(stubOnly = true)
	private Terminal terminal;
	@Mock
	private PrintWriter printWriter;
	private ConsoleChannel sut;

	@Before
	public void setup() {
		sut = new ConsoleChannel(terminal, Color.Red);
		given(terminal.writer()).willReturn(printWriter);
	}

	@Test
	public void empty() {
		sut.send(Record.empty());
		then(printWriter).should().println();
	}

	@Test
	public void oneValue() {
		sut.send(Record.of("key", Values.ofText("foo")));
		then(printWriter).should().append(Color.Red.ansi());
		then(printWriter).should().append("foo");
		then(printWriter).should().append(Color.Reset.ansi());
		then(printWriter).should().println();
	}

	@Test
	public void twoValues() {
		sut.send(Record.of("key", Values.ofText("foo")).append("another_key", Values.ofText("bar")));
		then(printWriter).should().append(Color.Red.ansi());
		then(printWriter).should().append("foo");
		then(printWriter).should().append(" ");
		then(printWriter).should().append("bar");
		then(printWriter).should().append(Color.Reset.ansi());
		then(printWriter).should().println();
	}
}
