package org.hosh.runtime;

import java.util.Optional;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;

/**
 * This is a special-case channel used as first command of a
 * pipeline and in the REPL.
 */
public class NullChannel implements Channel {
	@Override
	public Optional<Record> recv() {
		return Optional.empty();
	}

	@Override
	public void send(Record record) {
	}
}
