package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class RecordTest {
	@Test
	public void copy() {
		Record a = Record.of("key", Values.ofText("a"));
		Record b = Record.copy(a);
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.toString()).isEqualTo(b.toString());
	}

	@Test
	public void valueObject() {
		Record a = Record.empty();
		Record b = a;
		Record c = a.append("test", Values.ofText("a"));
		assertThat(a).isEqualTo(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(b).isNotEqualTo(c);
	}

	@Test
	public void mutation() {
		Record a = Record.empty();
		Record b = Record.copy(a).append("key", Values.ofText("a"));
		assertThat(a).isNotEqualTo(b);
		assertThat(a).isNotSameAs(b);
	}

	@Test
	public void representation() {
		Record a = Record.empty();
		assertThat(a.toString()).isEqualTo("Record[data={}]");
		Record b = a.append("size", Values.ofHumanizedSize(10));
		assertThat(b.toString()).isEqualTo("Record[data={size=Size[10B]}]");
	}

	@Test
	public void retainInsertOrder() {
		Value value = Values.ofText("value");
		Value anotherValue = Values.ofText("another_value");
		Record a = Record.empty().append("key", value).append("another_key", anotherValue).prepend("first", value);
		assertThat(a.keys()).containsExactly("first", "key", "another_key");
		assertThat(a.values()).containsExactly(value, value, anotherValue);
	}

	@Test
	public void addNonUniqueKey() {
		Value value = Values.ofText("value");
		Value anotherValue = Values.ofText("another_value");
		Record a = Record.of("kkk", value);
		assertThatThrownBy(() -> {
			a.append("kkk", anotherValue);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicated key: kkk");
	}

	@Test
	public void ofNonUniqueKey() {
		Value value = Values.ofText("value");
		Value anotherValue = Values.ofText("another_value");
		assertThatThrownBy(() -> {
			Record.of("kkk", value, "kkk", anotherValue);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicated key: kkk");
	}

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(Record.class).verify();
	}
}
