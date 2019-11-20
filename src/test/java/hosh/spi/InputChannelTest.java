package hosh.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class InputChannelTest {

	@Mock(stubOnly = true)
	private InputChannel in;

	@Mock(stubOnly = true)
	private Record record;

	@Test
	public void empty() {
		given(in.recv()).willReturn(Optional.empty());
		Iterable<Record> iterable = InputChannel.iterate(in);
		assertThat(iterable).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void oneRecord() {
		given(in.recv()).willReturn(Optional.of(record), Optional.empty());
		Iterable<Record> iterable = InputChannel.iterate(in);
		assertThat(iterable).containsExactly(record);
	}

}
