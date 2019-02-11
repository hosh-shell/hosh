/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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

import org.hosh.doc.Experimental;

/**
 * A command specialization that performs set-up and clean-up.
 */
public interface CommandWrapper<T> extends Command {
	/**
	 * Create and set-up a resource.
	 */
	T before(List<String> args, Channel in, Channel out, Channel err);

	/**
	 * Clean-up the resource.
	 */
	void after(T resource, Channel in, Channel out, Channel err);

	@Experimental(description = "retry the inner start if this method reports true, it has been abused to create 'repeat'")
	default boolean retry(@SuppressWarnings("unused") T resource) {
		return false;
	}

	@Override
	default ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		throw new IllegalStateException("implementation will be provided by the compiler");
	}
}
