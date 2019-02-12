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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hosh.spi.Record;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PipelineChannelTest {
	@Mock(stubOnly = true)
	private Record one;

	@Test
	public void stopConsumer() {
		PipelineChannel sut = new PipelineChannel();
		sut.send(one);
		sut.stopConsumer();
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(one);
		Optional<Record> recv2 = sut.recv();
		assertThat(recv2).isEmpty();
	}

	@Test
	public void sendRecv() {
		PipelineChannel sut = new PipelineChannel();
		sut.send(one);
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(one);
	}

	@Test
	public void stringRepr() { // this is quite important while debugging
		PipelineChannel sut = new PipelineChannel();
		assertThat(sut).hasToString("PipelineChannel[done=false,queue=[]]");
		sut.send(one);
		assertThat(sut).hasToString("PipelineChannel[done=false,queue=[one]]");
		sut.stopProducer();
		assertThat(sut).hasToString("PipelineChannel[done=true,queue=[one]]");
		sut.consumeAnyRemainingRecord();
		assertThat(sut).hasToString("PipelineChannel[done=true,queue=[]]");
	}
}
