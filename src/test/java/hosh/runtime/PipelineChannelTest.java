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
package hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hosh.runtime.PipelineChannel;
import hosh.spi.Record;
import hosh.testsupport.WithThread;

@ExtendWith(MockitoExtension.class)
public class PipelineChannelTest {

	@RegisterExtension
	public final WithThread withThread = new WithThread();

	@Mock(stubOnly = true)
	private Record record;

	@Test
	public void stopConsumer() {
		PipelineChannel sut = new PipelineChannel();
		sut.send(record);
		sut.stopConsumer();
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(record);
		Optional<Record> recv2 = sut.recv();
		assertThat(recv2).isEmpty();
	}

	@Test
	public void sendRecv() {
		PipelineChannel sut = new PipelineChannel();
		sut.send(record);
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(record);
	}

	@Test
	public void sendInterrupted() {
		PipelineChannel sut = new PipelineChannel();
		Thread.currentThread().interrupt();
		sut.send(record);
		boolean stillInterrupted = Thread.currentThread().isInterrupted();
		assertThat(stillInterrupted).isTrue();
	}

	@Test
	public void recvInterrupted() {
		PipelineChannel sut = new PipelineChannel();
		Thread.currentThread().interrupt();
		Optional<Record> recv = sut.recv();
		assertThat(recv).isEmpty();
	}

	@Test
	public void stringRepr() { // this is quite important while debugging
		PipelineChannel sut = new PipelineChannel();
		assertThat(sut).hasToString("PipelineChannel[done=false,queue=[]]");
		sut.send(record);
		assertThat(sut).hasToString("PipelineChannel[done=false,queue=[record]]");
		sut.stopProducer();
		assertThat(sut).hasToString("PipelineChannel[done=true,queue=[record]]");
		sut.consumeAnyRemainingRecord();
		assertThat(sut).hasToString("PipelineChannel[done=true,queue=[]]");
	}
}
