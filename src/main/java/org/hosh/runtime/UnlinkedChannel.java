package org.hosh.runtime;

import java.util.Optional;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;

public class UnlinkedChannel implements Channel {
	@Override
	public Optional<Record> recv() {
		throw new UnsupportedOperationException("cannot read from this channel");
	}

	@Override
	public void send(Record record) {
		throw new UnsupportedOperationException("cannot write to this channel");
	}
}
