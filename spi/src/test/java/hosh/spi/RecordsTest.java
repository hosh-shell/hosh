/*
 * MIT License
 *
 * Copyright (c) 2019-2021 Davide Angelocola
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
package hosh.spi;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordsTest {

	@Test
	void copy() {
		Record a = Records.singleton(Keys.NAME, Values.ofText("a"));
		Record b = Records.singleton(Keys.NAME, Values.ofText("a"));
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a).hasToString(b.toString());
	}

	@Test
	void valueObject() {
		Record a = Records.empty();
		Record c = a.append(Keys.NAME, Values.ofText("a"));
		assertThat(a).isEqualTo(a);
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(c);
	}

	@Test
	void mutation() {
		Record a = Records.empty();
		Record b = Records.singleton(Keys.NAME, Values.ofText("a"));
		assertThat(a).isNotEqualTo(b);
		assertThat(a).isNotSameAs(b);
	}

	@Test
	void asString() {
		Record a = Records.empty();
		assertThat(a).hasToString("Record[data={}]");
		Record b = a.append(Keys.SIZE, Values.ofSize(10));
		assertThat(b).hasToString("Record[data={Key['size']=Size[10B]}]");
		Record c = b.append(Keys.NAME, Values.ofText("foo"));
		assertThat(c).hasToString("Record[data={Key['size']=Size[10B],Key['name']=Text[foo]}]");
	}

	@Test
	void retainInsertOrder() {
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
	void equalsContract() {
		EqualsVerifier.forClass(Records.Empty.class).verify();
		EqualsVerifier.forClass(Records.Singleton.class).withNonnullFields("entry").verify();
		EqualsVerifier.forClass(Records.Generic.class).withNonnullFields("entries").verify();
		EqualsVerifier.forClass(Record.Entry.class).verify();
	}

	@Test
	void emptyWithGenericEmpty() {
		Record empty = Records.empty();
		Record generic = Records.builder().build(); // instance of Generic but empty
		assertThat(empty).isEqualTo(generic);
		assertThat(generic).isEqualTo(empty);
	}

	@Test
	void singletonEqualsGenericAndViceVersa() {
		Record singleton = Records.singleton(Keys.COUNT, Values.ofNumeric(1));
		Record generic = Records.builder().entry(Keys.COUNT, Values.ofNumeric(1)).build();
		assertThat(singleton).isEqualTo(generic);
		assertThat(generic).isEqualTo(singleton);
	}

	@Test
	void value() {
		Record record = Records.empty();
		assertThat(record.value(Keys.NAME)).isEmpty();
		record = record.append(Keys.NAME, Values.none());
		assertThat(record.value(Keys.NAME)).isNotEmpty().contains(Values.none());
		record = record.prepend(Keys.INDEX, Values.ofNumeric(1));
		assertThat(record.keys()).containsExactly(Keys.INDEX, Keys.NAME);
	}

	@Test
	void instanceIsDetachedFromBuilder() {
		Records.Builder builder = Records.builder();
		builder.entry(Keys.NAME, Values.none());
		Record record = builder.build();
		builder.entry(new Record.Entry(Keys.COUNT, Values.none())); // please note that this entry has been added after build()
		assertThat(record.value(Keys.NAME)).isPresent();
		assertThat(record.value(Keys.COUNT)).isEmpty();
	}

	@Test
	void empty() {
		Record a = Records.empty();
		assertThat(a.keys()).isEmpty();
		assertThat(a.values()).isEmpty();
		assertThat(a.entries()).isEmpty();
	}

	@Test
	void singleton() {
		Value name = Values.ofText("a name");
		Record a = Records.singleton(Keys.NAME, name);
		assertThat(a.keys()).containsExactly(Keys.NAME);
		assertThat(a.values()).containsExactly(name);
		assertThat(a.entries()).containsExactly(new Record.Entry(Keys.NAME, name));
		assertThat(a.value(Keys.NAME)).isPresent().hasValue(name);
		assertThat(a.value(Keys.COUNT)).isNotPresent();
	}

	@Test
	void generic() {
		Record a = Records.singleton(Keys.NAME, Values.none()).prepend(Keys.COUNT, Values.none());
		assertThat(a.keys()).containsExactly(Keys.COUNT, Keys.NAME);
		assertThat(a.values()).containsExactly(Values.none(), Values.none());
		assertThat(a.entries()).containsExactly(new Record.Entry(Keys.COUNT, Values.none()), new Record.Entry(Keys.NAME, Values.none()));
	}

	@Test
	void entry() {
		Record.Entry entry = new Record.Entry(Keys.NAME, Values.none());
		assertThat(entry).hasToString("Entry[key=Key['name'],value=None]");
		assertThat(entry.getKey()).isEqualTo(Keys.NAME);
		assertThat(entry.getValue()).isEqualTo(Values.none());
	}

	@Test
	void prependAndAppend() {
		Record a = Records.empty();
		Value text = Values.ofText("some text");
		assertThat(a.prepend(Keys.TEXT, text)).isEqualTo(a.append(Keys.TEXT, text));
		assertThat(a.append(Keys.TEXT, text)).isEqualTo(a.prepend(Keys.TEXT, text));
	}

}
