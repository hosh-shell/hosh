package org.hosh.runtime;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;

/**
 * This is a special-case channel used as first command of a
 * pipeline and in the REPL.
 */
public class NullChannel implements Channel {
	@Override
	public void send(Record record) {
	}
}
