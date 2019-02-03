package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import org.hosh.spi.Record;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PipeChannelTest {
	@Mock
	private Record one;

	@Test
	public void stopConsumer() {
		PipeChannel sut = new PipeChannel(new LinkedBlockingQueue<Record>(2));
		sut.send(one);
		sut.stopConsumer();
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(one);
		Optional<Record> recv2 = sut.recv();
		assertThat(recv2).isEmpty();
	}

	@Test
	public void stopProducer() {
		PipeChannel sut = new PipeChannel(new LinkedBlockingQueue<Record>(2));
		sut.stopProducer();
		boolean done = sut.trySend(one);
		assertThat(done).isTrue();
	}

	@Test
	public void trySend() {
		PipeChannel sut = new PipeChannel(new LinkedBlockingQueue<Record>(2));
		boolean done = sut.trySend(one);
		assertThat(done).isFalse();
		Optional<Record> recv1 = sut.recv();
		assertThat(recv1).contains(one);
	}
}
