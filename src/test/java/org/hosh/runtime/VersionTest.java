package org.hosh.runtime;

import java.io.IOException;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class VersionTest {

	@Test
	public void readVersion() throws IOException {
		String readVersion = Version.readVersion();
		assertThat(readVersion).isNotBlank();
	}

}
