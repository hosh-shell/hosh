package org.hosh.spi;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Value object representing a mutable record of k/v pairs.
 * 
 * Records are created, modified and finally consumed to create pipelines.
 * E.g. listing files of a directory produces a stream of records with name, size, permissions.
 * The user can select a subset of these keys (e.g. name) and then it can be presented to the screen.
 * 
 * TODO: emit a special record at the start to provide column names 
 * TODO: type safety by using actual Java classes
 */
@NotThreadSafe
public class  Record {

    private final Map<String, Object> data;

    private Record(Map<String, Object> data) {
        this.data = new LinkedHashMap<>(data);
    }

    public static Record copy(@Nonnull Record record) {
        return new Record(record.data);
    }

    public static Record empty() {
        return new Record(new HashMap<>(0));
    }

    public Record add(@Nonnull String key, @Nonnull Object value) {
        data.put(key, value);
        return this;
    }
    
    public Stream<String> values() {
    	// TODO: toString() is subject to locale (e.g. numbers or dates)
    	return data.values().stream().map(Objects::toString);
    }
    
    @SafeVarargs
    public final Record select(@Nonnull String ...keys) {
    	Record result = Record.empty();
    	for (String key : keys) {
    		if (this.data.containsKey(key)) {
    			result.add(key, data.get(key));
    		}
    	}
    	return result;
    }

    @Override
    public String toString() {
        return String.format("Record[data=%s]", data);
    }

}
