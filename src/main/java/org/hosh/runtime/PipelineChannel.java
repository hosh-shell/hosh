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
package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import org.hosh.spi.Channel;
import org.hosh.spi.Keys;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Record;
import org.hosh.spi.Values;

public class PipelineChannel implements Channel {
	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private static final boolean QUEUE_FAIRNESS = false;
	private static final int QUEUE_CAPACITY = 100;
	private final Record poisonPill = Record.of(Keys.of("poisonpill"), Values.none());
	private final BlockingQueue<Record> queue;
	private volatile boolean done;

	public PipelineChannel() {
		queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY, QUEUE_FAIRNESS);
		done = false;
	}

	@Override
	public Optional<Record> recv() {
		if (done) {
			return Optional.empty();
		}
		try {
			LOGGER.finer("waiting for record... ");
			Record record = queue.take();
			if (record == poisonPill) {
				LOGGER.finer("got POISON_PILL... ");
				done = true;
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
		if (done) {
			throw new ProducerPoisonPill();
		}
		LOGGER.finer("sending record");
		try {
			queue.put(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void stopProducer() {
		done = true;
		LOGGER.fine("producer stop requested");
	}

	public void stopConsumer() {
		if (!done) {
			send(poisonPill);
		}
	}

	public void consumeAnyRemainingRecord() {
		LOGGER.finer("consuming remaining records");
		List<Record> consumer = new ArrayList<>();
		queue.drainTo(consumer);
		LOGGER.finer("done consuming remaining records");
	}

	@Override
	public String toString() {
		return String.format("PipelineChannel[done=%s,queue=%s]", done, queue);
	}

	/**
	 * Since send() is a void method an exception is needed
	 * to forcibly stop a producer.
	 *
	 * This is the dual of poison pill record, sent by a producer to
	 * stop any downstream readers.
	 */
	public static class ProducerPoisonPill extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
