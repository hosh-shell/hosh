package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hosh.spi.Values.Unit;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ValuesTest {

	@Test
	public void text() {
		EqualsVerifier.forClass(Values.Text.class).verify();
	}

	@Test
	public void size() {
		EqualsVerifier.forClass(Values.Size.class).verify();

		assertThat(Values.ofHumanizedSize(0L)).hasToString("Size[0B]");
		assertThat(Values.ofHumanizedSize(512L)).hasToString("Size[512B]");
		assertThat(Values.ofHumanizedSize(1023L)).hasToString("Size[1023B]");
		assertThat(Values.ofHumanizedSize(1024L)).hasToString("Size[1KB]");
		assertThat(Values.ofHumanizedSize(1024L * 1024)).hasToString("Size[1MB]");
		assertThat(Values.ofHumanizedSize(1024L * 1024 * 2 - 1)).hasToString("Size[1MB]"); // TODO: should be 1.99MB
		assertThat(Values.ofHumanizedSize(1024L * 1024 * 1024)).hasToString("Size[1GB]");

		assertThatThrownBy(() -> Values.ofSize(-1, Unit.B))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("negative size");

		assertThatThrownBy(() -> Values.ofHumanizedSize(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("negative size");
	}

	@Test
	public void localPath() {
		EqualsVerifier.forClass(Values.Size.class).verify();
	}

}
