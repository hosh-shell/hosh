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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hosh.spi.Record.Entry;

public class Records {

	private Records() {
	}

	public static Record empty() {
		return new Records.Empty();
	}

	public static Record singleton(Key key, Value value) {
		return new Records.Singleton(key, value);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Mutable builder of Record objects. Retains insertion order.
	 */
	public static class Builder {

		private final ArrayList<Entry> data = new ArrayList<>();

		private Builder() {
		}

		public Builder entry(Key key, Value value) {
			data.add(new Entry(key, value));
			return this;
		}

		public Record build() {
			return new Generic(data.toArray(Entry[]::new));
		}
	}

	static class Empty implements Record {

		@Override
		public List<Key> keys() {
			return Collections.emptyList();
		}

		@Override
		public List<Value> values() {
			return Collections.emptyList();
		}

		@Override
		public List<Record.Entry> entries() {
			return Collections.emptyList();
		}

		@Override
		public Record append(Key key, Value value) {
			return new Records.Singleton(key, value);
		}

		@Override
		public Record prepend(Key key, Value value) {
			return new Records.Singleton(key, value);
		}

		@Override
		public Optional<Value> value(Key key) {
			return Optional.empty();
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public final int hashCode() {
			return 17;
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Record) {
				Record that = (Record) obj;
				return that.size() == 0;
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return "Record[data={}]";
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			// no-op
		}
	}

	static class Singleton implements Record {

		private final Key key;

		private final Value value;

		public Singleton(Key key, Value value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public List<Key> keys() {
			return Collections.singletonList(key);
		}

		@Override
		public List<Value> values() {
			return Collections.singletonList(value);
		}

		@Override
		public List<Record.Entry> entries() {
			return Collections.singletonList(new Record.Entry(key, value));
		}

		@Override
		public Record append(Key newKey, Value newValue) {
			return new Builder()
					.entry(this.key, this.value)
					.entry(newKey, newValue)
					.build();
		}

		@Override
		public Record prepend(Key newKey, Value newValue) {
			return new Builder()
					.entry(newKey, newValue)
					.entry(this.key, this.value)
					.build();
		}

		@Override
		public Optional<Value> value(Key wantedKey) {
			if (Objects.equals(this.key, wantedKey)) {
				return Optional.of(value);
			} else {
				return Optional.empty();
			}
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public final int hashCode() {
			return Objects.hash(key, value);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Record) {
				Record that = (Record) obj;
				return that.size() == 1 && this.entries().equals(that.entries());
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return String.format("Record[data={%s=%s}]", key, value);
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			value.print(printWriter, locale);
		}
	}

	static class Generic implements Record {

		private final Entry[] entries;

		private Generic(Entry[] entries) {
			if (entries == null) {
				throw new IllegalArgumentException("data cannot be null");
			}
			this.entries = entries;
		}

		@Override
		public Record append(Key key, Value value) {
			Entry[] newEntries = Arrays.copyOf(entries, entries.length + 1);
			newEntries[entries.length] = new Entry(key, value);
			return new Generic(newEntries);
		}

		@Override
		public Record prepend(Key key, Value value) {
			Entry[] newEntries = new Entry[entries.length + 1];
			newEntries[0] = new Entry(key, value);
			for (int i = 0; i < entries.length; i++) {
				newEntries[i + 1] = entries[i];
			}
			return new Generic(newEntries);
		}

		@Override
		public List<Key> keys() {
			return Arrays
					.stream(entries)
					.map(Entry::getKey)
					.collect(Collectors.toUnmodifiableList());
		}

		@Override
		public List<Value> values() {
			return Arrays
					.stream(entries)
					.map(Entry::getValue)
					.collect(Collectors.toUnmodifiableList());
		}

		@Override
		public List<Entry> entries() {
			return List.of(entries);
		}

		@Override
		public Optional<Value> value(Key key) {
			for (Entry entry : entries) {
				if (entry.getKey().equals(key)) {
					return Optional.of(entry.getValue());
				}
			}
			return Optional.empty();
		}

		@Override
		public int size() {
			return entries.length;
		}

		@Override
		public final int hashCode() {
			return Objects.hash((Object[]) entries);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Record) {
				Record that = (Record) obj;
				return this.size() == that.size() && this.entries().equals(that.entries());
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return String.format("Record[data={%s}]",
					Arrays
							.stream(entries)
							.map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
							.collect(Collectors.joining(",")));
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			Iterator<Value> iterator = values().iterator();
			while (iterator.hasNext()) {
				Value value = iterator.next();
				value.print(printWriter, locale);
				if (iterator.hasNext()) {
					printWriter.append(" ");
				}
			}
		}
	}
}
