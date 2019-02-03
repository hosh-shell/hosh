package org.hosh.spi;

import java.util.Optional;

/**
 * Handling of input/output of records.
 */
public interface Channel {
	void send(Record record);

	default Optional<Record> recv() {
		return Optional.empty();
	}
}
