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

import java.util.Locale;
import java.util.Optional;

/**
 * The value in @{see Record}.
 * NB: all concrete classes implementing this interface should be value objects.
 */
public interface Value extends Comparable<Value> {

	String show(Locale locale);

	/**
	 * Access to the underlying value. This can be used to convert a value to a more primitive type.
	 *
	 * @param type the class representing wanted type
	 * @param <T>  the wanted type
	 * @return optionally result value
	 */
	default <T> Optional<T> unwrap(Class<T> type) {
		return Optional.empty();
	}

	/**
	 * Merge two values to produce at most one value.
	 *
	 * @param value the value to merge with
	 * @return optionally a new value holding the result of the merge
	 */
	default Optional<Value> merge(Value value) {
		return Optional.empty();
	}

}
