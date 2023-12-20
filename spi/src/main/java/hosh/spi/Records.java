/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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

import hosh.spi.Record.Entry;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Records {

	private Records() {
	}

	public static Record empty() {
		return new Records.Empty();
	}

	public static Record singleton(Key key, Value value) {
		return new Records.Singleton(new Entry(key, value));
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

		// reuse existing entry (can be shared because it is immutable)
		public Builder entry(Entry entry) {
			data.add(entry);
			return this;
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
		public Stream<Key> keys() {
			return Stream.empty();
		}

		@Override
		public Stream<Value> values() {
			return Stream.empty();
		}

		@Override
		public Stream<Record.Entry> entries() {
			return Stream.empty();
		}

		@Override
		public Record append(Key key, Value value) {
			return new Records.Singleton(new Entry(key, value));
		}

		@Override
		public Record prepend(Key key, Value value) {
			return new Records.Singleton(new Entry(key, value));
		}

		@Override
		public Optional<Value> value(Key key) {
			// trivially returns empty optional
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
			if (obj instanceof Record that) {
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

		private final Entry entry;

		public Singleton(Entry entry) {
			this.entry = entry;
		}

		@Override
		public Stream<Key> keys() {
			return Stream.of(entry).map(Entry::getKey);
		}

		@Override
		public Stream<Value> values() {
			return Stream.of(entry).map(Entry::getValue);
		}

		@Override
		public Stream<Record.Entry> entries() {
			return Stream.of(entry);
		}

		@Override
		public Record append(Key newKey, Value newValue) {
			return new Builder()
				.entry(entry)
				.entry(newKey, newValue)
				.build();
		}

		@Override
		public Record prepend(Key newKey, Value newValue) {
			return new Builder()
				.entry(newKey, newValue)
				.entry(entry)
				.build();
		}

		@Override
		public Optional<Value> value(Key wantedKey) {
			if (Objects.equals(this.entry.getKey(), wantedKey)) {
				return Optional.of(this.entry.getValue());
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
			return Objects.hash(entry);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Record that) {
                return that.size() == 1 && Records.equals(this.entries(), that.entries());
			} else {
				return false;
			}
		}

		@Override
		public String toString() {
			return String.format("Record[data={%s=%s}]", entry.getKey(), entry.getValue());
		}

		@Override
		public void print(PrintWriter printWriter, Locale locale) {
			entry.getValue().print(printWriter, locale);
		}
	}

	static class Generic implements Record {

		private final Entry[] entries;

		private Generic(Entry[] entries) {
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
			System.arraycopy(entries, 0, newEntries, 1, entries.length);
			return new Generic(newEntries);
		}

		@Override
		public Stream<Key> keys() {
			return Arrays
				.stream(entries)
				.map(Entry::getKey);
		}

		@Override
		public Stream<Value> values() {
			return Arrays
				.stream(entries)
				.map(Entry::getValue);
		}

		@Override
		public Stream<Entry> entries() {
			return Stream.of(entries);
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
			return Arrays.hashCode(entries);
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Record that) {
                return this.size() == that.size() && Records.equals(this.entries(), that.entries());
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

	// size has been checked before calling this method (and that is a very cheap check)
	// this is a trivial implementation to check for equality two streams
	private static boolean equals(Stream<Entry> s1, Stream<Entry> s2) {
		Iterator<Entry> it1 = s1.iterator();
		Iterator<Entry> it2 = s2.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			if (!Objects.equals(it1.next(), it2.next())) {
				return false;
			}
		}
		if (it1.hasNext() || it2.hasNext()) {
			throw new IllegalStateException("different lengths");
		}
		return true;
	}
}
