package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hosh.doc.Experimental;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineChannel implements Channel {
	private static final Logger LOGGER = LoggerFactory.getLogger(PipelineChannel.class);
	private static final Record POISON_PILL = Record.of("__POISON_PILL__", null);
	private static final boolean QUEUE_FAIRNESS = true;
	private static final int QUEUE_CAPACITY = 10;
	private final BlockingQueue<Record> queue;
	private final AtomicBoolean done;

	public PipelineChannel() {
		queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY, QUEUE_FAIRNESS);
		done = new AtomicBoolean(false);
	}

	@Override
	public Optional<Record> recv() {
		try {
			if (done.getAcquire()) {
				return Optional.empty();
			}
			LOGGER.trace("waiting for record... ");
			Record record = queue.take();
			if (POISON_PILL.equals(record)) {
				LOGGER.trace("got POISON_PILL... ");
				done.compareAndSet(false, true);
				return Optional.empty();
			}
			LOGGER.trace("got record {}", record);
			return Optional.of(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	@Override
	public void send(Record record) {
		if (done.getAcquire()) {
			throw new ProducerPoisonPill();
		}
		LOGGER.trace("sending record {}", record);
		try {
			queue.put(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// sent by consumer to signal "do not produce anything"
	public void stopProducer() {
		done.compareAndSet(false, true);
		LOGGER.debug("producer stop requested");
	}

	// sent by the producer to signal "end of channel"
	public void stopConsumer() {
		if (!done.getAcquire()) {
			send(POISON_PILL);
		}
	}

	// let the producer to get unblocked in put()
	public void consumeAnyRemainingRecord() {
		LOGGER.trace("consuming remaining records");
		List<Record> consumer = new ArrayList<>();
		queue.drainTo(consumer);
		LOGGER.trace("done consuming remaining records");
	}

	@Experimental(description = "best solution found so far to stop a very fast producer")
	public static class ProducerPoisonPill extends RuntimeException {
		private static final long serialVersionUID = 1L;

		// stacktrace is not needed
		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
