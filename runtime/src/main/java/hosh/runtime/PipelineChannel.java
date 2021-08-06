/*
 * MIT License
 *
 * Copyright (c) 2019-2021 Davide Angelocola
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
package hosh.runtime;

import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;

import java.util.Optional;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PipelineChannel implements InputChannel, OutputChannel {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private final Record poisonPill = Records.singleton(Keys.of("poisonpill"), Values.none());

	private final LinkedTransferQueue<Record> queue;

	private volatile boolean done;

	public PipelineChannel() {
		queue = new LinkedTransferQueue<>();
		done = false;
	}

	@Override
	public Optional<Record> recv() {
		try {
			LOGGER.finer("waiting for record...");
			Record record = queue.take();
			if (record == poisonPill) {
				LOGGER.finer("got poison pill");
				return Optional.empty();
			}
			LOGGER.finer("got record");
			return Optional.of(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	@Override
	public void send(Record record) {
		LOGGER.finer("sending record");
		if (done) {
			throw new ProducerPoisonPill();
		}
		try {
			do {
				boolean transferred = queue.tryTransfer(record, 50, TimeUnit.MILLISECONDS);
				if (transferred) {
					return;
				}
				LOGGER.finer("send failed, retry...");
			} while (queue.hasWaitingConsumer());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void stopProducer() {
		LOGGER.fine("producer stop requested");
		done = true;
	}

	public void stopConsumer() {
		LOGGER.fine("consumer stop requested");
		done = true;
		queue.add(poisonPill);
	}

	// Since send() is a void method an exception is needed
	// to forcibly stop a producer.
	public static class ProducerPoisonPill extends RuntimeException {

		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Throwable fillInStackTrace() {
			// this is really control-flow exception, no need for stacktrace
			return this;
		}
	}
}
