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

	@Experimental(description = "downstream command can signal upstream to stop producing items")
	default void requestStop() {
		// by default it is ignored
	}

	@Experimental(description = "upstream should use this method to 'nice' and stop producing items")
	default boolean trySend(Record record) {
		// by default send everything
		send(record);
		return false;
	}
}
