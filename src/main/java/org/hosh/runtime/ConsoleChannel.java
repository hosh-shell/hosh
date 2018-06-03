package org.hosh.runtime;

import java.io.PrintStream;

import javax.annotation.Nonnull;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;

public class ConsoleChannel implements Channel {

	private final PrintStream ps;

	public ConsoleChannel(@Nonnull PrintStream ps) {
		this.ps = ps;
	}

	@Override
	public void send(Record record) {
		if (ps == System.err)
			ps.println("\\u001b[31m" + record);
		else 
			ps.println(record);
	}

}
