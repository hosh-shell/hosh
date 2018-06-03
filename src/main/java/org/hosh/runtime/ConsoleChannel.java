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
	private final String userStyle;
	private final String defaultStyle;

	public ConsoleChannel(@Nonnull Terminal terminal, @Nonnull AttributedStyle userStyle) {
		this.terminal = terminal;
		this.userStyle = new AttributedStringBuilder().style(userStyle).toAnsi(terminal);
		this.defaultStyle = new AttributedStringBuilder().style(AttributedStyle.DEFAULT).toAnsi(terminal);
	}

	@Override
	public void send(@Nonnull Record record) {
		terminal.writer().println(record.values().collect(Collectors.joining(" ", userStyle, defaultStyle)));
	}

}
