package org.hosh.spi;

import java.io.PrintStream;
import java.util.Locale;

import javax.annotation.Nonnull;

public class SimpleChannel implements Channel {

	private final PrintStream st;

	public SimpleChannel(@Nonnull PrintStream st) {
		this.st = st;
	}

	@Override
	public void send(Record record) {
		StringBuilder output = new StringBuilder(); 
		for (Value value : record.values()) {
			value.append(output, Locale.getDefault());
			output.append(" ");
		}
		st.append(output);
	}

}
