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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An immutable, persistent value object representing a record of key-value
 * pairs.
 */
public interface Record {
	Record append(String key, Value value);

	Record prepend(String key, Value value);

	Collection<String> keys();

	Collection<Value> values();

	Collection<Entry> entries();

	Optional<Value> value(String key);

	static Record empty() {
		return new Record.Empty();
	}

	static Record of(String key, Value value) {
		return new Record.Singleton(key, value);
	}

	static Record of(String key1, Value value1, String key2, Value value2) {
		return new Record.Generic().append(key1, value1).append(key2, value2);
	}

	class Entry {
		private final String key;
		private final Value value;

		public Entry(String key, Value value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public Value getValue() {
			return value;
		}
	}

	class Empty implements Record {
		@Override
		public Collection<String> keys() {
			return Collections.emptyList();
		}

		@Override
		public Collection<Value> values() {
			return Collections.emptyList();
		}

		@Override
		public Collection<Entry> entries() {
			return Collections.emptyList();
		}

		@Override
		public Record append(String key, Value value) {
			return new Singleton(key, value);
		}

		@Override
		public Record prepend(String key, Value value) {
			return new Singleton(key, value);
		}

		@Override
		public Optional<Value> value(String key) {
			return Optional.empty();
		}

		@Override
		public final int hashCode() {
			return 17;
		}

		@Override
		public final boolean equals(Object obj) {
			return obj instanceof Empty;
		}

		@Override
		public String toString() {
			return "Record[data={}]";
		}
	}

	static class Singleton implements Record {
		private final String key;
		private final Value value;

		public Singleton(String key, Value value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public Collection<String> keys() {
			return Collections.singletonList(key);
		}

		@Override
		public Collection<Value> values() {
			return Collections.singletonList(value);
		}

		@Override
		public Collection<Entry> entries() {
			return Collections.singletonList(new Entry(key, value));
		}

		@Override
		public Record append(String key2, Value value2) {
			return new Generic().append(this.key, this.value).append(key2, value2);
		}

		@Override
		public Record prepend(String key2, Value value2) {
			return new Generic().prepend(this.key, this.value).prepend(key2, value2);
		}

		@Override
		public Optional<Value> value(String key1) {
			if (Objects.equals(this.key, key1)) {
				return Optional.of(value);
			} else {
				return Optional.empty();
			}
		}

		@Override
		public final int hashCode() {
			return Objects.hash(key, value);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Singleton) {
				Singleton that = (Singleton) obj;
				return Objects.equals(this.key, that.key) && Objects.equals(this.value, that.value);
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return String.format("Record[data={%s=%s}]", key, value);
		}
	}

	static class Generic implements Record {
		private final Map<String, Value> data;

		private Generic() {
			this(new LinkedHashMap<>(0));
		}

		private Generic(Map<String, Value> data) {
			this.data = data;
		}

		@Override
		public Record append(String key, Value value) {
			Map<String, Value> copy = new LinkedHashMap<>(data.size() + 1);
			copy.putAll(this.data);
			addUniqueMapping(copy, key, value);
			return new Generic(copy);
		}

		@Override
		public Record prepend(String key, Value value) {
			Map<String, Value> copy = new LinkedHashMap<>(data.size() + 1);
			copy.put(key, value);
			copy.putAll(this.data);
			return new Generic(copy);
		}

		private static void addUniqueMapping(Map<String, Value> data, String key, Value value) {
			Value previous = data.put(key, value);
			if (previous != null) {
				throw new IllegalArgumentException("duplicated key: " + key);
			}
		}

		@Override
		public Collection<String> keys() {
			return data.keySet();
		}

		@Override
		public Collection<Value> values() {
			return data.values();
		}

		@Override
		public Collection<Entry> entries() {
			return data
					.entrySet()
					.stream()
					.map(kv -> new Entry(kv.getKey(), kv.getValue()))
					.collect(Collectors.toList());
		}

		@Override
		public Optional<Value> value(String key) {
			return Optional.ofNullable(data.get(key));
		}

		@Override
		public String toString() {
			return String.format("Record[data=%s]", data);
		}

		@Override
		public final int hashCode() {
			return Objects.hash(data);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Generic) {
				Generic that = (Generic) obj;
				return Objects.equals(this.data, that.data);
			} else {
				return false;
			}
		}
	}
}
