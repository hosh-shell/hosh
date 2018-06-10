package org.hosh.spi;

import java.io.PrintStream;
import java.util.Locale;

import javax.annotation.Nonnull;

public class SimpleChannel implements Channel {

	private final PrintStream printStream;

	public SimpleChannel(@Nonnull PrintStream printStream) {
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
