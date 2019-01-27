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
		writer.append(Color.Reset.ansi);
		writer.println();
	}

	public enum Color {
		Black("\u001b[30m"), Red("\u001b[31m"), Green("\u001b[32m"), Yellow("\u001b[33m"), Blue("\u001b[34m"), Magenta("\u001b[35m"), Cyan(
				"\u001b[36m"), White("\u001b[37m"), Reset("\u001b[0m");
		private final String ansi;

		private Color(String ansi) {
			this.ansi = ansi;
		}

		public String ansi() {
			return ansi;
		}
	}
}
