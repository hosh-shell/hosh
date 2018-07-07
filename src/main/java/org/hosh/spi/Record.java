package org.hosh.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.hosh.doc.Todo;

/**
 * A value object representing a record of k/v pairs.
 *
 * Records are created, modified and finally consumed to create pipelines. E.g.
 * listing files of a directory produces a stream of records with name, size,
 * permissions. The user can select a subset of these keys (e.g. name) and then
 * it can be presented to the screen somehow.
 */
@Todo(description = "emit a special record at the start to provide column names")
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

	public Record add(String key, Value value) {
		Map<String, Value> copy = new LinkedHashMap<>(data);
		copy.put(key, value);
		return new Record(copy);
	}

	public Collection<String> keys() {
		return Collections.unmodifiableCollection(data.keySet());
	}

	public Collection<Value> values() {
		return Collections.unmodifiableCollection(data.values());
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
