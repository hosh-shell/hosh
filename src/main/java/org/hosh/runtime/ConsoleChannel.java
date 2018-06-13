package org.hosh.runtime;

import java.util.Iterator;
import java.util.Locale;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Value;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class ConsoleChannel implements Channel {

	private final Terminal terminal;
	private final int color;

	public ConsoleChannel(Terminal terminal, int color) {
		this.terminal = terminal;
		this.color = color;
	}

	@Override
	public void send(Record record) {
		StringBuilder output = new StringBuilder();
		Iterator<Value> values = record.values().iterator();
		while (values.hasNext()) {
			Value value = values.next();
			value.append(output, Locale.getDefault());
			if (values.hasNext()) {
				output.append(" ");
			}
		}
		String ansiString = new AttributedStringBuilder()
				.style(AttributedStyle.DEFAULT.foreground(color))
				.append(output)
				.style(AttributedStyle.DEFAULT)
				.toAnsi(terminal);
		terminal.writer().println(ansiString);
	}

}
