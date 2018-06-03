package org.hosh.runtime;

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.jline.terminal.Terminal;

public class ConsoleChannel implements Channel {

	private final Terminal terminal;

	public ConsoleChannel(@Nonnull Terminal terminal) {
		this.terminal= terminal;
	}

	@Override
	public void send(Record record) {
		terminal.writer().println(record.values().collect(Collectors.joining(" ")));
	}

}
