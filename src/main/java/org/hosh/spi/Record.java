package org.hosh.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A value object representing a record of k/v pairs.
 *
 * Records are created, modified and finally consumed to create pipelines. E.g.
 * listing files of a directory produces a stream of records with name, size,
 * permissions. The user can select a subset of these keys (e.g. name) and then
 * it can be presented to the screen somehow.
 */
public final class Record {
	private final Map<String, Value> data;

	private Record(Map<String, Value> data) {
		this.data = data;
	}

	public static Record copy(Record record) {
		return new Record(new LinkedHashMap<>(record.data));
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

	public Record add(String key, Value value) {
		Map<String, Value> copy = new LinkedHashMap<>(data);
		addUniqueMapping(copy, key, value);
		return new Record(copy);
	}

	private static void addUniqueMapping(Map<String, Value> data, String key2, Value value2) {
		Value previous = data.put(key2, value2);
		if (previous != null) {
			throw new IllegalArgumentException("duplicated key: " + key2);
		}
	}

	public Collection<String> keys() {
		return Collections.unmodifiableCollection(data.keySet());
	}

	public Collection<Value> values() {
		return Collections.unmodifiableCollection(data.values());
	}

	public Optional<Value> value(String key) {
		return Optional.ofNullable(data.get(key));
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
