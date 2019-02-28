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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An immutable, persistent value object representing
 * a list of key-value pairs.
 */
public interface Record {
	/**
	 * Yields a new Record with the specified mapping as last one.
	 */
	Record append(String key, Value value);

	/**
	 * Yields a new Record with the specified mapping as first one.
	 */
	Record prepend(String key, Value value);

	List<String> keys();

	List<Value> values();

	List<Entry> entries();

	Optional<Value> value(String key);

	static Record empty() {
		return new Record.Empty();
	}

	static Record of(String key, Value value) {
		return new Record.Singleton(key, value);
	}

	static Builder builder() {
		return new Builder();
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

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Entry) {
				Entry that = (Entry) obj;
				return Objects.equals(this.key, that.key) && Objects.equals(this.value, that.value);
			} else {
				return false;
			}
		}

		@Override
		public final int hashCode() {
			return Objects.hash(key, value);
		}

		@Override
		public String toString() {
			return String.format("Entry[key=%s,value=%s]", key, value);
		}
	}

	class Empty implements Record {
		@Override
		public List<String> keys() {
			return Collections.emptyList();
		}

		@Override
		public List<Value> values() {
			return Collections.emptyList();
		}

		@Override
		public List<Entry> entries() {
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
		public List<String> keys() {
			return Collections.singletonList(key);
		}

		@Override
		public List<Value> values() {
			return Collections.singletonList(value);
		}

		@Override
		public List<Entry> entries() {
			return Collections.singletonList(new Entry(key, value));
		}

		@Override
		public Record append(String newKey, Value newValue) {
			return new Builder()
					.entry(this.key, this.value)
					.entry(newKey, newValue)
					.build();
		}

		@Override
		public Record prepend(String newKey, Value newValue) {
			return new Builder()
					.entry(newKey, newValue)
					.entry(this.key, this.value)
					.build();
		}

		@Override
		public Optional<Value> value(String wantedKey) {
			if (Objects.equals(this.key, wantedKey)) {
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

		private Generic(Map<String, Value> data) {
			this.data = data;
		}

		@Override
		public Record append(String key, Value value) {
			Map<String, Value> copy = new LinkedHashMap<>(data.size() + 1);
			copy.putAll(this.data);
			copy.put(key, value);
			return new Generic(copy);
		}

		@Override
		public Record prepend(String key, Value value) {
			Map<String, Value> copy = new LinkedHashMap<>(data.size() + 1);
			copy.put(key, value);
			copy.putAll(this.data);
			return new Generic(copy);
		}

		@Override
		public List<String> keys() {
			return data
					.keySet()
					.stream()
					.collect(Collectors.toUnmodifiableList());
		}

		@Override
		public List<Value> values() {
			return data
					.values()
					.stream()
					.collect(Collectors.toUnmodifiableList());
		}

		@Override
		public List<Entry> entries() {
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

		@Override
		public String toString() {
			return String.format("Record[data=%s]", data);
		}
	}

	/**
	 * Mutable builder of Record objects. Retains insertion order.
	 */
	class Builder {
		private final LinkedHashMap<String, Value> data = new LinkedHashMap<>(2);

		private Builder() {
		}

		public Builder entry(String key, Value value) {
			data.put(key, value);
			return this;
		}

		public Record build() {
			return new Record.Generic(new LinkedHashMap<>(data));
		}
	}
}
