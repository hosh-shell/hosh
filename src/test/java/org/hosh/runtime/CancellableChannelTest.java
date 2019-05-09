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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

import java.util.concurrent.CancellationException;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CancellableChannelTest {

	@Mock(stubOnly = true)
	private Record record;

	@Mock
	private Channel channel;

	@InjectMocks
	private CancellableChannel sut;

	@Test
	public void recv() {
		sut.recv();
		then(channel).should().recv();
	}

	@Test
	public void send() {
		sut.send(record);
		then(channel).should().send(record);
	}

	@Test
	public void recvInterrupted() {
		Thread.currentThread().interrupt();
		assertThatThrownBy(() -> sut.recv())
				.hasMessage("interrupted")
				.isInstanceOf(CancellationException.class);
	}

	@Test
	public void sendInterrupted() {
		Thread.currentThread().interrupt();
		assertThatThrownBy(() -> sut.send(record))
				.hasMessage("interrupted")
				.isInstanceOf(CancellationException.class);
	}
}
