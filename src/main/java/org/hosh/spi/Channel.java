package org.hosh.spi;

import java.util.Optional;

import org.hosh.doc.Experimental;

/**
 * Handling of input/output of records.
 */
public interface Channel {
	void send(Record record);

	default Optional<Record> recv() {
		return Optional.empty();
	}

	@Experimental(description = "consumer command can signal producer to halt")
	default void stopProducer() {
		// by default it is ignored
	}

	@Experimental(description = "producer should use this method to exit as soon as possible after a consumer called requestStop")
	default boolean trySend(Record record) {
		// by default send everything
		send(record);
		return false;
	}
}
