package org.hosh.runtime;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipeChannel implements Channel {
	private final Logger logger = LoggerFactory.getLogger(PipeChannel.class);
	private final Record poisonPill = Record.of("__POISON_PILL__", null);
	private final BlockingQueue<Record> queue;
	private AtomicBoolean done = new AtomicBoolean(false);

	public PipeChannel(BlockingQueue<Record> queue) {
		this.queue = queue;
	}

	@Override
	public Optional<Record> recv() {
		try {
			if (done.getAcquire()) {
				return Optional.empty();
			}
			logger.trace("waiting for record... ");
			Record record = queue.take();
			if (poisonPill.equals(record)) {
				logger.trace("got POISON_PILL... ");
				done.compareAndSet(false, true);
				return Optional.empty();
			}
			logger.trace("got record {}", record);
			return Optional.of(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	@Override
	public void send(Record record) {
		if (done.getAcquire()) {
			logger.trace("channel is done, not sending {}", record);
			throw new ProducerPoisonPill();
		}
		logger.trace("sending record {}", record);
		try {
			queue.put(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// sent by consumer to signal "do not produce anything"
	public void stopProducer() {
		done.compareAndSet(false, true);
		logger.debug("producer stop requested, done={}", done);
	}

	// sent by the producer to signal "end of channel"
	public void stopConsumer() {
		if (!done.getAcquire()) {
			send(poisonPill);
		}
	}

	// let the producer to get unblocked in put()
	public void consumeAnyRemainingRecord() {
		logger.trace("consuming remaining records");
		queue.drainTo(new ArrayList<Record>());
	}

	public static class ProducerPoisonPill extends RuntimeException {
		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
