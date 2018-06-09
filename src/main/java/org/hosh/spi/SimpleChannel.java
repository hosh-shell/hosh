package org.hosh.spi;

import java.io.PrintStream;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class SimpleChannel implements Channel {

	private final PrintStream st;

	public SimpleChannel(@Nonnull PrintStream st) {
		this.st = st;
	}

	@Override
	public void send(Record record) {
		st.println(record.values().collect(Collectors.joining(" ")));
	}

}
