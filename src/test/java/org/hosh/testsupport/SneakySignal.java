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
package org.hosh.testsupport;

import org.hosh.doc.Bug;
import org.hosh.doc.Todo;

@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "allows some unit tests")
public class SneakySignal {
	private SneakySignal() {
	}

	@Todo(description = "using reflection to avoid checkstyle error, need to migrate to a proper api")
	public static void raise(String signalName) throws ReflectiveOperationException, InterruptedException {
		Class<?> signalClass = Class.forName("sun.misc.Signal");
		Object signal = signalClass.getConstructor(String.class).newInstance(signalName);
		signalClass.getMethod("raise", signalClass).invoke(null, signal);
		// this is inherently unstable and ugly but it is needed
		// to make sure the signal is delivered to this process
		Thread.sleep(100);
	}
}
