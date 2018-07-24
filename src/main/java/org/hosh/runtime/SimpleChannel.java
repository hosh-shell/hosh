package org.hosh.runtime;

import java.io.PrintStream;
import java.util.Locale;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Value;

public class SimpleChannel implements Channel {

	private final PrintStream printStream;

	public SimpleChannel(PrintStream printStream) {
		this.printStream = printStream;
	}

	@Override
	public void send(Record record) {
		StringBuilder output = new StringBuilder();
		for (Value value : record.values()) {
			value.append(output, Locale.getDefault());
			output.append(" ");
		}
		printStream.append(output);
	}

}
