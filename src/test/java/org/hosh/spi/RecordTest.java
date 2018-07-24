package org.hosh.spi;

import static org.assertj.core.api.Assertions.*;

import org.hosh.spi.Values.Unit;
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

		Record c = a.add("test", Values.ofText("a"));
		assertThat(a).isEqualTo(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(b).isNotEqualTo(c);
	}

	@Test
	public void mutation() {
		Record a = Record.empty();
		Record b = Record.copy(a).add("key", Values.ofText("a"));

		assertThat(a).isNotEqualTo(b);
		assertThat(a).isNotSameAs(b);
	}

	@Test
	public void representation() {
		Record a = Record.empty();
		assertThat(a.toString()).isEqualTo("Record[data={}]");
		Record b = a.add("size", Values.ofSize(10, Unit.MB));
		assertThat(b.toString()).isEqualTo("Record[data={size=Size[10MB]}]");
	}

	@Test
	public void retainInsertOrder() {
		Value value = Values.ofText("value");
		Value anotherValue = Values.ofText("another_value");
		Record a = Record.empty().add("key", value).add("another_key", anotherValue);
		assertThat(a.keys()).containsExactly("key", "another_key");
		assertThat(a.values()).containsExactly(value, anotherValue);
	}

	@Test
	public void addNonUniqueKey() {
		Value value = Values.ofText("value");
		Value anotherValue = Values.ofText("another_value");
		Record a = Record.of("kkk", value);
		assertThatThrownBy(() -> {
			a.add("kkk", anotherValue);
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
