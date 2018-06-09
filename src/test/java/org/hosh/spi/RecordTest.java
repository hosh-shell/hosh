package org.hosh.spi;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RecordTest {

	@Test
	public void copy() {
		Record a = Record.of("key", 1);
		Record b = Record.copy(a);

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
		assertThat(a.toString()).isEqualTo(b.toString());
	}

	@Test
	public void valueObject() {
		Record a = Record.empty();
		Record b = a;

		Record c = a.add("test", a);
		assertThat(a).isEqualTo(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(b).isNotEqualTo(c);
	}

	@Test
	public void mutation() {
		Record a = Record.empty();
		Record b = Record.copy(a).add("key", 1);

		assertThat(a).isNotEqualTo(b);
		assertThat(a).isNotSameAs(b);
	}

	@Test
	public void equality() {
		Record a = Record.empty();

		assertThat(a).isNotEqualTo("");
	}

	@Test
	public void representation() {
		Record a = Record.empty();
		assertThat(a.toString()).isEqualTo("Record[data={}]");
		Record b = a.add("test", 1);
		assertThat(b.toString()).isEqualTo("Record[data={test=1}]");
	}

}
