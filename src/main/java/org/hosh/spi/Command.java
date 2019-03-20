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

import java.util.List;
import java.util.Optional;

import org.hosh.doc.Experimental;

/**
 * Command represents a built-in (i.e. ls) or system commands (i.e. vim).
 */
public interface Command {

	ExitStatus run(List<String> args, Channel in, Channel out, Channel err);

	default <T> Optional<T> downCast(Class<T> requiredClass) {
		if (requiredClass.isInstance(this)) {
			return Optional.of(requiredClass.cast(this));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Describe command in a human readable form. By default this is the class name
	 * of the command.
	 *
	 * @return a human readable description of the command
	 *         (e.g. 'ls' or '/usr/bin/cat')
	 */
	@Experimental(description = "mandatory marking as experimental")
	default String describe() {
		return this.getClass().getSimpleName();
	}
}
