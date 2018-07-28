package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

public class VersionTest {
	@Test
	public void readVersion() throws IOException {
		String readVersion = Version.readVersion();
		assertThat(readVersion).isNotBlank();
	}
}
