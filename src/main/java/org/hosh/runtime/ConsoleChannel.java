package org.hosh.runtime;

import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
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
		String output = record.values().collect(Collectors.joining(" "));
		terminal.writer().println(
				new AttributedStringBuilder()
						.style(AttributedStyle.DEFAULT.foreground(color))
						.append(output)
						.style(AttributedStyle.DEFAULT)
						.toAnsi(terminal));
	}

}
