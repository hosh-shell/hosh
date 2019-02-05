/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hosh.spi.Record.Empty;
import org.hosh.spi.Record.Generic;
import org.hosh.spi.Record.Singleton;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class RecordTest {
	@Test
	public void copy() {
		Record a = Record.of("key", Values.ofText("a"));
		Record b = Record.of("key", Values.ofText("a"));
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
		Record b = Record.of("key", Values.ofText("a"));
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
		EqualsVerifier.forClass(Empty.class).verify();
		EqualsVerifier.forClass(Singleton.class).verify();
		EqualsVerifier.forClass(Generic.class).verify();
	}
}
