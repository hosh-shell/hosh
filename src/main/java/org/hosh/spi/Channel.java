package org.hosh.spi;

import java.util.Optional;

/**
 * Handling of input/output of records.
 */
public interface Channel {
	default Optional<Record> recv() {
		return Optional.empty();
	}

	void send(Record record);
}
