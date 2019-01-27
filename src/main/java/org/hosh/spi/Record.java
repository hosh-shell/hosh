package org.hosh.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, persistent value object representing a record of key-value
 * pairs.
 */
public final class Record {
	private final Map<String, Value> data;

	private Record(Map<String, Value> data) {
		this.data = data;
	}

	public static Record empty() {
		return new Record(new LinkedHashMap<>(0));
	}

	public static Record of(String key, Value value) {
		Map<String, Value> data = new LinkedHashMap<>(1);
		data.put(key, value);
		return new Record(data);
	}

	public static Record of(String key1, Value value1, String key2, Value value2) {
		Map<String, Value> data = new LinkedHashMap<>(2);
		data.put(key1, value1);
		addUniqueMapping(data, key2, value2);
		return new Record(data);
	}

	public Record append(String key, Value value) {
		Map<String, Value> copy = new LinkedHashMap<>(data.size() + 1);
		copy.putAll(this.data);
		addUniqueMapping(copy, key, value);
		return new Record(copy);
	}

	public Record prepend(String key, Value value) {
		Map<String, Value> copy = new LinkedHashMap<>(data.size() + 1);
		copy.put(key, value);
		copy.putAll(this.data);
		return new Record(copy);
	}

	private static void addUniqueMapping(Map<String, Value> data, String key, Value value) {
		Value previous = data.put(key, value);
		if (previous != null) {
			throw new IllegalArgumentException("duplicated key: " + key);
		}
	}

	public Collection<String> keys() {
		return Collections.unmodifiableCollection(data.keySet());
	}

	public Collection<Value> values() {
		return Collections.unmodifiableCollection(data.values());
	}

	public Value value(String key) {
		return data.get(key);
	}

	@Override
	public String toString() {
		return String.format("Record[data=%s]", data);
	}

	@Override
	public int hashCode() {
		return Objects.hash(data);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Record) {
			Record that = (Record) obj;
			return Objects.equals(this.data, that.data);
		} else {
			return false;
		}
	}
}
