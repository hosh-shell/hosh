/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
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
		// Given
		Record a = Records.singleton(Keys.NAME, Values.ofText("a"));
		Record b = Records.singleton(Keys.NAME, Values.ofText("a"));

		// Then
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSameHashCodeAs(b);
		assertThat(a).hasToString(b.toString());
	}

	@Test
	void valueObject() {
		// Given
		Record a = Records.empty();

		// When
		Record c = a.append(Keys.NAME, Values.ofText("a"));

		// Then
		assertThat(a).isNotEqualTo(c);
		assertThat(a).isNotEqualTo(c);
	}

	@Test
	void mutation() {
		// Given
		Record a = Records.empty();
		Record b = Records.singleton(Keys.NAME, Values.ofText("a"));

		// Then
		assertThat(a).isNotEqualTo(b);
		assertThat(a).isNotSameAs(b);
	}

	@Test
	void asString() {
		// Given
		Record a = Records.empty();

		// Then
		assertThat(a).hasToString("Record[data={}]");
		Record b = a.append(Keys.SIZE, Values.ofSize(10));
		assertThat(b).hasToString("Record[data={Key['size']=Size[10B]}]");
		Record c = b.append(Keys.NAME, Values.ofText("foo"));
		assertThat(c).hasToString("Record[data={Key['size']=Size[10B],Key['name']=Text[foo]}]");
	}

	@Test
	void retainInsertOrder() {
		// Given
		Value value = Values.ofText("value");
		Value anotherValue = Values.ofText("another_value");
		Value lastValue = Values.ofText("lastValue");

		// When
		Record a = Records
				.empty()
				.append(Keys.of("key"), value)
				.append(Keys.of("anotherkey"), anotherValue)
				.prepend(Keys.of("first"), value)
				.append(Keys.of("last"), lastValue);

		// Then
		assertThat(a.keys()).containsExactly(Keys.of("first"), Keys.of("key"), Keys.of("anotherkey"), Keys.of("last"));
		assertThat(a.values()).containsExactly(value, value, anotherValue, lastValue);
	}

	@Test
	void equalsContract() {
		// When / Then
		EqualsVerifier.forClass(Records.Empty.class).verify();
		EqualsVerifier.forClass(Records.Singleton.class).withNonnullFields("entry").verify();
		EqualsVerifier.forClass(Records.Generic.class).withNonnullFields("entries").verify();
		EqualsVerifier.forClass(Record.Entry.class).withNonnullFields("key", "value").verify();
	}

	@Test
	void emptyWithGenericEmpty() {
		// Given
		Record empty = Records.empty();
		Record generic = Records.builder().build(); // instance of Generic but empty

		// Then
		assertThat(empty).isEqualTo(generic);
		assertThat(generic).isEqualTo(empty);
	}

	@Test
	void singletonEqualsGenericAndViceVersa() {
		// Given
		Record singleton = Records.singleton(Keys.COUNT, Values.ofNumeric(1));
		Record generic = Records.builder().entry(Keys.COUNT, Values.ofNumeric(1)).build();

		// Then
		assertThat(singleton).isEqualTo(generic);
		assertThat(generic).isEqualTo(singleton);
	}

	@Test
	void value() {
		// Given
		Record record = Records.empty();

		// Then
		assertThat(record.value(Keys.NAME)).isEmpty();
		record = record.append(Keys.NAME, Values.none());
		assertThat(record.value(Keys.NAME)).isNotEmpty().contains(Values.none());
		record = record.prepend(Keys.INDEX, Values.ofNumeric(1));
		assertThat(record.keys()).containsExactly(Keys.INDEX, Keys.NAME);
	}

	@Test
	void instanceIsDetachedFromBuilder() {
		// Given
		Records.Builder builder = Records.builder();
		builder.entry(Keys.NAME, Values.none());
		Record record = builder.build();
		builder.entry(new Record.Entry(Keys.COUNT, Values.none())); // please note that this entry has been added after build()

		// Then
		assertThat(record.value(Keys.NAME)).isPresent();
		assertThat(record.value(Keys.COUNT)).isEmpty();
	}

	@Test
	void empty() {
		// When
		Record a = Records.empty();

		// Then
		assertThat(a.keys()).isEmpty();
		assertThat(a.values()).isEmpty();
		assertThat(a.entries()).isEmpty();
	}

	@Test
	void singleton() {
		// Given
		Value name = Values.ofText("a name");

		// When
		Record a = Records.singleton(Keys.NAME, name);

		// Then
		assertThat(a.keys()).containsExactly(Keys.NAME);
		assertThat(a.values()).containsExactly(name);
		assertThat(a.entries()).containsExactly(new Record.Entry(Keys.NAME, name));
		assertThat(a.value(Keys.NAME)).isPresent().hasValue(name);
		assertThat(a.value(Keys.COUNT)).isNotPresent();
	}

	@Test
	void generic() {
		// When
		Record a = Records.singleton(Keys.NAME, Values.none()).prepend(Keys.COUNT, Values.none());

		// Then
		assertThat(a.keys()).containsExactly(Keys.COUNT, Keys.NAME);
		assertThat(a.values()).containsExactly(Values.none(), Values.none());
		assertThat(a.entries()).containsExactly(new Record.Entry(Keys.COUNT, Values.none()), new Record.Entry(Keys.NAME, Values.none()));
	}

	@Test
	void entry() {
		// When
		Record.Entry entry = new Record.Entry(Keys.NAME, Values.none());

		// Then
		assertThat(entry).hasToString("Entry[key=Key['name'],value=None]");
		assertThat(entry.key()).isEqualTo(Keys.NAME);
		assertThat(entry.value()).isEqualTo(Values.none());
	}

	@Test
	void prependAndAppend() {
		// Given
		Record a = Records.empty();
		Value text = Values.ofText("some text");

		// Then
		assertThat(a.prepend(Keys.TEXT, text)).isEqualTo(a.append(Keys.TEXT, text));
		assertThat(a.append(Keys.TEXT, text)).isEqualTo(a.prepend(Keys.TEXT, text));
	}

}
