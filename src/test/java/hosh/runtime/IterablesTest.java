package hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import hosh.runtime.Iterables.Producer;

@ExtendWith(MockitoExtension.class)
public class IterablesTest {

	@Mock
	private Producer<Object> producer;

	@Test
	public void empty() {
		given(producer.produce()).willReturn(Optional.empty());
		Iterable<Object> iterable = Iterables.over(producer);
		assertThat(iterable).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void oneElement() {
		given(producer.produce()).willReturn(Optional.of("1"), Optional.empty());
		Iterable<Object> iterable = Iterables.over(producer);
		assertThat(iterable).containsExactly("1");
	}
}
