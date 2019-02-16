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
package org.hosh.runtime;

import java.util.Optional;
import java.util.concurrent.CancellationException;

import org.hosh.doc.Experimental;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;

@Experimental(description = "this could be a wrong solution but right now is very cheap to implement:" +
		"perhaps a better solution would be to let Channel and Command to throw InterruptedException")
public class InterruptionChannel implements Channel {
	private final Channel channel;

	public InterruptionChannel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void send(Record record) {
		// this is needed to let ctrl-C interrupt the currently running thread
		if (Thread.currentThread().isInterrupted()) {
			throw new CancellationException("thread has been interrupted by ctrl-C");
		}
		channel.send(record);
	}

	@Override
	public Optional<Record> recv() {
		return channel.recv();
	}
}
