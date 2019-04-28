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
package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.PrintWriter;

import org.hosh.spi.Ansi;
import org.hosh.spi.Keys;
import org.hosh.spi.Records;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConsoleChannelTest {

	@Mock(stubOnly = true)
	private Terminal terminal;

	@Mock
	private PrintWriter printWriter;

	private ConsoleChannel sut;

	@BeforeEach
	public void setup() {
		given(terminal.writer()).willReturn(printWriter);
		sut = new ConsoleChannel(terminal, Ansi.Style.NONE);
	}

	@Test
	public void empty() {
		sut.send(Records.empty());
		then(printWriter).should().append(System.lineSeparator());
	}

	@Test
	public void oneValue() {
		sut.send(Records.singleton(Keys.NAME, Values.ofText("foo")));
		then(printWriter).should().append("foo");
		then(printWriter).should().append(System.lineSeparator());
	}

	@Test
	public void twoValues() {
		sut.send(Records.builder().entry(Keys.NAME, Values.ofText("foo")).entry(Keys.VALUE, Values.ofText("bar")).build());
		then(printWriter).should().append("foo");
		then(printWriter).should().append(" ");
		then(printWriter).should().append("bar");
		then(printWriter).should().append(System.lineSeparator());
	}

	@Test
	public void repr() {
		assertThat(sut).hasToString("ConsoleChannel[style=NONE]");
	}
}
