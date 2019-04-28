/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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

import org.hosh.spi.Record.Entry;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class RecordsTest {

	@Test
	public void copy() {
		Record a = Records.singleton(Keys.NAME, Values.ofText("a"));
		Record b = Records.singleton(Keys.NAME, Values.ofText("a"));
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a.toString()).isEqualTo(b.toString());
	}

	@Test
	public void valueObject() {
		Record a = Records.empty();
		Record b = a;
		Record c = b.append(Keys.NAME, Values.ofText("a"));
		assertThat(a).isEqualTo(b);
		assertThat(a).isNotEqualTo(c);
		assertThat(b).isNotEqualTo(c);
	}

	@Test
	public void mutation() {
		Record a = Records.empty();
		Record b = Records.singleton(Keys.NAME, Values.ofText("a"));
		assertThat(a).isNotEqualTo(b);
		assertThat(a).isNotSameAs(b);
	}

	@Test
	public void representation() {
		Record a = Records.empty();
		assertThat(a.toString()).isEqualTo("Record[data={}]");
		Record b = a.append(Keys.SIZE, Values.ofHumanizedSize(10));
		assertThat(b.toString()).isEqualTo("Record[data={Key[size]=Size[10B]}]");
		Record c = b.append(Keys.NAME, Values.ofText("foo"));
		assertThat(c.toString()).isEqualTo("Record[data={Key[size]=Size[10B], Key[name]=Text[foo]}]");
	}

	@Test
	public void retainInsertOrder() {
		Value value = Values.ofText("value");
		Value anotherValue = Values.ofText("another_value");
		Value lastValue = Values.ofText("lastValue");
		Record a = Records
				.empty()
				.append(Keys.of("key"), value)
				.append(Keys.of("anotherkey"), anotherValue)
				.prepend(Keys.of("first"), value)
				.append(Keys.of("last"), lastValue);
		assertThat(a.keys()).containsExactly(Keys.of("first"), Keys.of("key"), Keys.of("anotherkey"), Keys.of("last"));
		assertThat(a.values()).containsExactly(value, value, anotherValue, lastValue);
	}

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(Records.Empty.class).verify();
		EqualsVerifier.forClass(Records.Singleton.class).verify();
		EqualsVerifier.forClass(Records.Generic.class).withNonnullFields("data").verify();
		EqualsVerifier.forClass(Entry.class).verify();
	}

	@Test
	public void singletonEqualsGenericAndViceversa() {
		Record singleton = Records.singleton(Keys.COUNT, Values.ofNumeric(1));
		Record generic = Records.builder().entry(Keys.COUNT, Values.ofNumeric(1)).build();
		assertThat(singleton).isEqualTo(generic);
		assertThat(generic).isEqualTo(singleton);
	}

	@Test
	public void value() {
		Record record = Records.empty();
		assertThat(record.value(Keys.NAME)).isEmpty();
		record = record.append(Keys.NAME, Values.none());
		assertThat(record.value(Keys.NAME)).isNotEmpty().contains(Values.none());
	}

	@Test
	public void instanceIsDetachedFromBuilder() {
		Records.Builder builder = Records.builder();
		builder.entry(Keys.NAME, Values.none());
		Record record = builder.build();
		builder.entry(Keys.COUNT, Values.none()); // please note that this has been added after build()
		assertThat(record.value(Keys.NAME)).isPresent();
		assertThat(record.value(Keys.COUNT)).isEmpty();
	}

	@Test
	public void empty() {
		Record a = Records.empty();
		assertThat(a.keys()).isEmpty();
		assertThat(a.values()).isEmpty();
		assertThat(a.entries()).isEmpty();
	}

	@Test
	public void singleton() {
		Record a = Records.singleton(Keys.NAME, Values.none());
		assertThat(a.keys()).containsExactly(Keys.NAME);
		assertThat(a.values()).containsExactly(Values.none());
		assertThat(a.entries()).containsExactly(new Entry(Keys.NAME, Values.none()));
	}

	@Test
	public void generic() {
		Record a = Records.singleton(Keys.NAME, Values.none()).prepend(Keys.COUNT, Values.none());
		assertThat(a.keys()).containsExactly(Keys.COUNT, Keys.NAME);
		assertThat(a.values()).containsExactly(Values.none(), Values.none());
		assertThat(a.entries()).containsExactly(new Entry(Keys.COUNT, Values.none()), new Entry(Keys.NAME, Values.none()));
	}

	@Test
	public void entry() {
		Entry entry = new Entry(Keys.NAME, Values.none());
		assertThat(entry).hasToString("Entry[key=Key[name],value=None]");
		assertThat(entry.getKey()).isEqualTo(Keys.NAME);
		assertThat(entry.getValue()).isEqualTo(Values.none());
	}
}
