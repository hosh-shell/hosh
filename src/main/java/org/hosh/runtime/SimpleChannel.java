package org.hosh.runtime;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Locale;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Value;

/**
 * Output a record to a non-ANSI terminal, this is used only by
 * non-interactive shell.
 */
public class SimpleChannel implements Channel {
	private final PrintStream printStream;

	public SimpleChannel(PrintStream printStream) {
		this.printStream = printStream;
	}

	@Override
	public void send(Record record) {
		Iterator<Value> iterator = record.values().iterator();
		while (iterator.hasNext()) {
			Value value = iterator.next();
			value.append(printStream, Locale.getDefault());
			if (iterator.hasNext()) {
				printStream.append(" ");
			}
		}
		printStream.println();
	}
}
