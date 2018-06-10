package org.hosh.runtime;

import java.util.Locale;

import javax.annotation.Nonnull;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Value;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class ConsoleChannel implements Channel {

	private final Terminal terminal;
	private final int color;

	public ConsoleChannel(@Nonnull Terminal terminal, int color) {
		this.terminal = terminal;
		this.color = color;
	}

	@Override
	public void send(@Nonnull Record record) {
		StringBuilder output = new StringBuilder(); 
		for (Value value : record.values()) {
			value.append(output, Locale.getDefault());
			output.append(" ");
		}
		terminal.writer().println(
				new AttributedStringBuilder()
						.style(AttributedStyle.DEFAULT.foreground(color))
						.append(output)
						.style(AttributedStyle.DEFAULT)
						.toAnsi(terminal));
	}

}
