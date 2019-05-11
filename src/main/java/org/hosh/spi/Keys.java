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

import java.util.Objects;

/**
 * Standard keys used over and over through all commands. Usually such keys
 * convey some semantic information and some defaults regarding textual
 * representation in terminal.
 *
 * Built-in commands can use "private" keys when appropriate.
 */
public class Keys {

	/**
	 * Human readable name of something (e.g. name of an env variable).
	 */
	public static final Key NAME = Keys.of("name");

	/**
	 * Human readable description of something.
	 */
	public static final Key DESCRIPTION = Keys.of("description");

	/**
	 * Human readable value of something (e.g. value of env variable).
	 */
	public static final Key VALUE = Keys.of("value");

	/**
	 * A human readable error message.
	 */
	public static final Key ERROR = Keys.of("error");

	/**
	 * Denotes an unstructured text value. Usually text is the output of a native
	 * command but also some built-it commands are using this standard key (e.g.
	 * lines).
	 */
	public static final Key TEXT = Keys.of("text");

	/**
	 * Denotes a local path to system.
	 */
	public static final Key PATH = Keys.of("path");

	/**
	 * Denotes a value measured in bytes.
	 */
	public static final Key SIZE = Keys.of("size");

	/**
	 * A numeric value representing a count.
	 */
	public static final Key COUNT = Keys.of("count");

	/**
	 * A numeric value representing an index in a set of values
	 * (e.g. 'cmd | enumerate').
	 */
	public static final Key INDEX = Keys.of("index");

	/**
	 * Denotes a random generated value.
	 */
	public static final Key RAND = Keys.of("rand");

	/**
	 * Denotes a duration value (i.e. java.time.Duration).
	 */
	public static final Key DURATION = Keys.of("duration");

	/**
	 * Denotes an instant value (i.e. java.time.Instant).
	 */
	public static final Key TIMESTAMP = Keys.of("timestamp");

	private Keys() {
	}

	public static Key of(String key) {
		return new StringKey(key);
	}

	static final class StringKey implements Key {

		private final String name;

		public StringKey(String name) {
			if (name == null) {
				throw new IllegalArgumentException("name must be not null");
			}
			if (name.isBlank()) {
				throw new IllegalArgumentException("name must be not blank");
			}
			this.name = name;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public final boolean equals(Object obj) {
			if (obj instanceof Key) {
				final Key that = (Key) obj;
				return Objects.equals(this.name(), that.name());
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

		@Override
		public String toString() {
			return String.format("Key['%s']", this.name);
		}

		@Override
		public int compareTo(Key o) {
			return this.name.compareTo(o.name());
		}
	}
}
