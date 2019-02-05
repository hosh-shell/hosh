/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
