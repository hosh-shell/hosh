/*
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
package hosh.spi;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A possibly infinite stream of @{{@link Record}.
 */
public interface InputChannel {

	/**
	 * Yield next record in this channel, yields {@link Optional#empty()} to signal
	 * end of channel.
	 */
	Optional<Record> recv();

	/**
	 * Allow to use for-each statement. Consumes the input channel.
	 */
	static Iterable<Record> iterate(InputChannel in) {
		return () -> new InputChannelIterator(in);
	}

	class InputChannelIterator implements Iterator<Record> {

		private final InputChannel in;

		private Record next;

		public InputChannelIterator(InputChannel in) {
			this.in = in;
		}

		@Override
		public boolean hasNext() {
			if (next != null) {
				return true;
			}
			Optional<Record> maybeNext = in.recv();
			if (maybeNext.isPresent()) {
				next = maybeNext.get();
				return true;
			}
			return false;
		}

		@Override
		public Record next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			Record result = next;
			next = null;
			return result;
		}
	}
}
