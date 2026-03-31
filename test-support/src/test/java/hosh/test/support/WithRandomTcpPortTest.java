package hosh.test.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WithRandomTcpPortTest {

	@Test
	void usage() {
		// Given
		WithRandomTcpPort sut = new WithRandomTcpPort();

		// When
		int result = sut.getRandomTcpPort();

		// Then
		assertThat(result).isGreaterThan(0);
	}
}