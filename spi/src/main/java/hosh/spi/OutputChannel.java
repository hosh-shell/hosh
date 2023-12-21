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

import hosh.doc.Experimental;

import java.util.EnumSet;

/**
 * Channel used to communicate with user and other external programs.
 * <p>
 * Any special behavior regarding how to behave (e.g. back-pressure, how to stop
 * a producer, etc.) is left to the implementor class.
 */
public interface OutputChannel {

	void send(Record record);

	@Experimental(description = "testing send options")
	default void send(Record record, EnumSet<Option> ignore) {
		send(record);
	}

	@Experimental(description = "testing send options")
	enum Option {
		/**
		 * Send record without any intermediate buffering.
		 * This is useful for certain commands that cannot tolerate buffering (e.g. watch).
		 */
		DIRECT
	}
}
