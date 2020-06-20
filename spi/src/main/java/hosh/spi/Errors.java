/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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

import hosh.doc.Experimental;
import hosh.doc.Todo;

import java.util.Objects;

/**
 * Factory methods for common errors.
 */
public class Errors {

	/**
	 * Extract message from exception. If not present use "(no message)".
	 */
	public static Record message(Exception e) {
		return Records.singleton(Keys.ERROR, Values.ofText(Objects.toString(e.getMessage(), "(no message)")));
	}

	public static Record message(String fmt, Object... args) {
		return Records.singleton(Keys.ERROR, Values.ofText(String.format(fmt, args)));
	}

	@Todo(description = "adding more details (e.g. enum with type of arguments)")
	public static Record usage(String fmt, Object... args) {
		return message("usage: " + fmt, args);
	}

	private Errors() {
	}

}
