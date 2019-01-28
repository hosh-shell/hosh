package org.hosh.runtime;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Locale;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Value;
import org.jline.terminal.Terminal;

public class ConsoleChannel implements Channel {
	private final Terminal terminal;
	private final Color color;

	public ConsoleChannel(Terminal terminal, Color color) {
		this.terminal = terminal;
		this.color = color;
	}

	@Override
	public void send(Record record) {
		@SuppressWarnings("resource")
		PrintWriter writer = terminal.writer();
		writer.append(color.ansi);
		Iterator<Value> values = record.values().iterator();
		while (values.hasNext()) {
			Value value = values.next();
			value.append(writer, Locale.getDefault());
			if (values.hasNext()) {
				writer.append(" ");
			}
		}
		writer.append(Color.RESET.ansi);
		writer.println();
	}

	public enum Color {
		BLACK("\u001b[30m"), RED("\u001b[31m"), GREEN("\u001b[32m"), YELLOW("\u001b[33m"), BLUE("\u001b[34m"), MAGENTA("\u001b[35m"), CYAN(
				"\u001b[36m"), WHITE("\u001b[37m"), RESET("\u001b[0m");
		private final String ansi;

		private Color(String ansi) {
			this.ansi = ansi;
		}

		public String ansi() {
			return ansi;
		}
	}
}
