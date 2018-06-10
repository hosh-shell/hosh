package org.hosh.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.PrintWriter;

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

	@Mock
	private Terminal terminal;
	@Mock
	private PrintWriter printWriter;

	private ConsoleChannel sut;

	@Before
	public void setup() {
		int color = 1; // not important by now
		sut = new ConsoleChannel(terminal, color);
	}

	@Test
	public void empty() {
		given(terminal.writer()).willReturn(printWriter);
		sut.send(Record.empty());
		then(printWriter).should().println("");
	}

	@Test
	public void oneValue() {
		given(terminal.writer()).willReturn(printWriter);
		sut.send(Record.of("key", Values.ofText("foo")));
		then(printWriter).should().println("[38;5;2147483647mfoo[0m");
	}

	@Test
	public void twoValues() {
		given(terminal.writer()).willReturn(printWriter);
		sut.send(Record.of("key", Values.ofText("foo")).add("another_key", Values.ofText("bar")));
		then(printWriter).should().println("[38;5;2147483647mfoo bar[0m");
	}

}
