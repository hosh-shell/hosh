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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.NoSuchElementException;

import org.hosh.spi.State;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LineReaderIteratorTest {

	@Mock(stubOnly = true)
	private LineReader lineReader;

	@Mock(stubOnly = true)
	private State state;

	@InjectMocks
	private LineReaderIterator sut;

	@Test
	public void oneLine() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willReturn("1");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("1");
	}

	@Test
	public void twoLines() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willReturn("1", "2");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("1");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("2");
	}

	@Test
	public void hasNextIsIdempotent() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willReturn("1");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.hasNext()).isTrue(); // second call
		assertThat(sut.next()).isEqualTo("1");
	}

	@Test
	public void stopsAtEOF() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willThrow(new EndOfFileException("simulated EOF"));
		assertThat(sut.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> sut.next());
	}

	@Test
	public void killsCurrentLineAtINT() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willThrow(new UserInterruptException("simulated INT"));
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("");
	}
}
