package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.hosh.spi.ExitStatus.InvalidExitCode;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ExitStatusTest {
	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(ExitStatus.class).verify();
	}

	@Test(expected = InvalidExitCode.class)
	public void belowMin() {
		ExitStatus.of(-1);
	}

	@Test(expected = InvalidExitCode.class)
	public void aboveMax() {
		ExitStatus.of(256);
	}

	@Test
	public void repr() {
		assertThat(ExitStatus.of(42)).hasToString("ExitStatus[value=42]");
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

	@Test
	public void parseValid() {
		Optional<ExitStatus> parsed = ExitStatus.parse("1");
		assertThat(parsed.isPresent()).isTrue();
		assertThat(parsed.get().value()).isEqualTo(1);
	}

	@Test
	public void parseInvalidText() {
		Optional<ExitStatus> parsed = ExitStatus.parse("asd");
		assertThat(parsed.isPresent()).isFalse();
	}

	@Test
	public void parseValidText() {
		Optional<ExitStatus> parsed = ExitStatus.parse("-1");
		assertThat(parsed.isPresent()).isFalse();
	}
}
