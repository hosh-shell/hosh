package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import org.hosh.spi.Record;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class NullChannelTest {
	@Mock
	private Record record;
	@InjectMocks
	private NullChannel sut;

	@Test
	public void recv() {
		assertThat(sut.recv()).isEmpty();
	}

	@Test
	public void send() {
		sut.send(record);
		then(record).shouldHaveZeroInteractions();
	}
}
