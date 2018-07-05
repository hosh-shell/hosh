package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StateTest {

	@Test
	public void string() {
		assertThat(new State()).hasToString("State[cwd='null',id=0,commands={}]");
	}

}
