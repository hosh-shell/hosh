package org.hosh.runtime;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipeChannel implements Channel {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Record poisonPill = Record.of("__POISON_PILL__", null);
	private final BlockingQueue<Record> queue;
	private volatile boolean done = false;

	public PipeChannel(BlockingQueue<Record> queue) {
		this.queue = queue;
	}

	@Override
	public Optional<Record> recv() {
		try {
			if (done) {
				return Optional.empty();
			}
			logger.trace("waiting for record... ");
			Record record = queue.take();
			if (poisonPill.equals(record)) {
				logger.trace("got POISON_PILL... ");
				done = true;
				return Optional.empty();
			}
			logger.trace("got record {}", record);
			return Optional.ofNullable(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	@Override
	public void send(Record record) {
		logger.trace("sending record {}", record);
		try {
			queue.put(record);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void stopProducer() {
		done = true;
		logger.debug("stop requested, done={}", done);
	}

	@Override
	public boolean trySend(Record record) {
		if (done) {
			logger.trace("record {} not sent", record);
			return true;
		} else {
			send(record);
			logger.trace("record {} sent", record);
			return false;
		}
	}

	// sent by the producer to signal "end of channel"
	public void stopConsumer() {
		if (!done) {
			send(poisonPill);
		}
	}

	// let the producer to get unblocked in put()
	public void consumeAnyRemainingRecord() {
		logger.trace("consuming remaining records");
		while (true) {
			Optional<Record> incoming = this.recv();
			if (incoming.isEmpty()) {
				break;
			}
			logger.trace("  discarding {}", incoming.get());
		}
		logger.trace("consumed remaining items");
	}
}
