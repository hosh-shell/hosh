package org.hosh.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A value object representing a record of k/v pairs.
 * 
 * Records are created, modified and finally consumed to create pipelines. E.g.
 * listing files of a directory produces a stream of records with name, size,
 * permissions. The user can select a subset of these keys (e.g. name) and then
 * it can be presented to the screen somehow.
 */
// TODO: emit a special record at the start to provide column names
// TODO: type safety by using actual Java classes
@ThreadSafe
public class Record {

	private final Map<String, Object> data;

	private Record(Map<String, Object> data) {
		this.data = data;
	}

	public static Record copy(@Nonnull Record record) {
		return new Record(new LinkedHashMap<>(record.data));
	}

	public static Record empty() {
		return new Record(new LinkedHashMap<>(0));
	}

	public static Record of(@Nonnull String key, @Nonnull Object value) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>(1);
		data.put(key, value);
		return new Record(data);
	}

	public Record add(@Nonnull String key, @Nonnull Object value) {
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>(data);
		copy.put(key, value);
		return new Record(copy);
	}

	public Stream<String> values() {
		// TODO: toString() is subject to locale (e.g. numbers or dates)
		return data.values().stream().map(Objects::toString);
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
