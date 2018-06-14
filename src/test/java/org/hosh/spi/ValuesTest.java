package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;

import org.hosh.spi.Values.Unit;
import org.junit.Test;

public class ValuesTest {

	@Test
	public void text() {
		Value a = Values.ofText("message");
		Value b = Values.ofText("message");
		Value c = Values.ofText("another message");

		assertThat(a).isEqualTo(a);
		assertThat(a).isEqualTo(b);
		assertThat(b).isEqualTo(a);
		assertThat(a).isNotEqualTo(c);
		assertThat(b).isNotEqualTo(c);
		assertThat(a).isNotEqualTo("message");
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	public void size() {
		Value a = Values.ofSize(42, Unit.MB);
		Value b = Values.ofSize(42, Unit.MB);
		Value c = Values.ofSize(1, Unit.MB);

		assertThat(a).isEqualTo(a);
		assertThat(a).isEqualTo(b);
		assertThat(b).isEqualTo(a);
		assertThat(a).isNotEqualTo(c);
		assertThat(b).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(42L);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());

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
		Value a = Values.ofPath(Paths.get("/tmp/file.txt"));
		Value b = Values.ofPath(Paths.get("/tmp/file.txt"));
		Value c = Values.ofPath(Paths.get("file.txt"));

		assertThat(a).isEqualTo(a);
		assertThat(a).isEqualTo(b);
		assertThat(b).isEqualTo(a);
		assertThat(a).isNotEqualTo(c);
		assertThat(b).isNotEqualTo(c);
		assertThat(a).isNotEqualTo("message");
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

}
