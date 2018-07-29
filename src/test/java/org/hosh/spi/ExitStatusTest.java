package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ExitStatusTest {
	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(ExitStatus.class).verify();
	}

	@Test(expected = IllegalArgumentException.class)
	public void belowMin() {
		ExitStatus.of(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void aboveMax() {
		ExitStatus.of(256);
	}

	@Test
	public void of() {
		// Tautological test, but it is important to preserve this contract
		assertThat(ExitStatus.of(42).value()).isEqualTo(42);
	}

	@Test
	public void success() {
		assertThat(ExitStatus.success().value()).isEqualTo(0);
	}

	@Test
	public void error() {
		assertThat(ExitStatus.error().value()).isEqualTo(1);
	}
}
