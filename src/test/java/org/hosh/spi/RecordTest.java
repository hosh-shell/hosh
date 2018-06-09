package org.hosh.spi;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RecordTest {

	@Test
	public void nonEmpty() {
		Record a = Record.of("key", 1);

		assertThat(a).isEqualTo(a);
		assertThat(a.toString()).isEqualTo("Record[data={key=1}]");
	}

	@Test
	public void empty() {
		Record a = Record.empty();

		assertThat(a).isEqualTo(a);
		assertThat(a.toString()).isEqualTo("Record[data={}]");
	}

	@Test
	public void mutation() {
		Record a = Record.empty();
		Record b = Record.copy(a).add("key", 1);

		assertThat(a).isEqualTo(a);
		assertThat(a).isNotEqualTo(b);
	}

}
