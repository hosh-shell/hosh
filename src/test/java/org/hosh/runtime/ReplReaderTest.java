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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ReplReaderTest {

	@Mock(stubOnly = true)
	private LineReader lineReader;

	@Mock(stubOnly = true)
	private Prompt prompt;

	private ReplReader sut;

	@Before
	public void setup() {
		sut = new ReplReader(prompt, lineReader);
	}

	@Test
	public void oneLine() {
		given(prompt.compute()).willReturn("hosh>");
		given(lineReader.readLine(anyString())).willReturn("1");
		assertThat(sut.read()).hasValue("1");
	}

	@Test
	public void twoLines() {
		given(prompt.compute()).willReturn("hosh>");
		given(lineReader.readLine(anyString())).willReturn("1", "2");
		assertThat(sut.read()).hasValue("1");
		assertThat(sut.read()).hasValue("2");
	}

	@Test
	public void killsCurrentLineAtINT() {
		given(prompt.compute()).willReturn("hosh>");
		given(lineReader.readLine(anyString())).willThrow(new UserInterruptException("simulated INT"));
		assertThat(sut.read()).hasValue("");
	}

	@Test
	public void stopsAtEOF() {
		given(prompt.compute()).willReturn("hosh>");
		given(lineReader.readLine(anyString())).willThrow(new EndOfFileException("simulated EOF"));
		assertThat(sut.read()).isEmpty();
	}
}
