package org.hosh.runtime;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;

/**
 * Output a record to a non-ANSI terminal, this is used only by
 * non-interactive shell.
 */
public class SimpleChannel implements Channel {
	private final RecordWriter recordWriter;

	public SimpleChannel(Appendable appendable) {
		this.recordWriter = new RecordWriter(appendable);
	}

	@Override
	public void send(Record record) {
		recordWriter.writeValues(record);
	}
}
