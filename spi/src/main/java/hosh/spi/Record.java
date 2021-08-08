/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An immutable value object representing a collection of key/value pairs.
 * <p>
 * Iteration order is well defined (i.e. insertion order).
 */
public interface Record extends Printable {

	/**
	 * Yields a new Record with the specified mapping as last one.
	 */
	Record append(Key key, Value value);

	/**
	 * Yields a new Record with the specified mapping as first one.
	 */
	Record prepend(Key key, Value value);

	Stream<Key> keys();

	Stream<Value> values();

	Stream<Entry> entries();

	Optional<Value> value(Key key);

	int size();

	/**
	 * An immutable value object representing a key/value pair.
	 * Key and value cannot be null.
	 */
	class Entry {

		private final Key key;

		private final Value value;

		public Entry(Key key, Value value) {
			this.key = Objects.requireNonNull(key);
			this.value = Objects.requireNonNull(value);
		}

		public Key getKey() {
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
}
