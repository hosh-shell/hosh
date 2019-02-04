package org.hosh.runtime;

import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.jline.terminal.Terminal;

public class ConsoleChannel implements Channel {
	private final RecordWriter recordWriter;

	public ConsoleChannel(Terminal terminal, Color color) {
		this.recordWriter = new RecordWriter(terminal.writer());
		this.recordWriter.setPrefix(color.ansi);
		this.recordWriter.setSuffix(Color.RESET.ansi);
	}

	@Override
	public void send(Record record) {
		recordWriter.writeValues(record);
	}

	public enum Color {
		BLACK("\u001b[30m"), RED("\u001b[31m"), GREEN("\u001b[32m"), YELLOW("\u001b[33m"), BLUE("\u001b[34m"), MAGENTA("\u001b[35m"), CYAN(
				"\u001b[36m"), WHITE("\u001b[37m"), RESET("\u001b[0m");
		private final String ansi;

		Color(String ansi) {
			this.ansi = ansi;
		}

		public String ansi() {
			return ansi;
		}
	}
}
