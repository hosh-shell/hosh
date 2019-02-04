package org.hosh.runtime;

import java.io.IOError;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hosh.spi.State;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class LineReaderIterator implements Iterator<String> {
	private final State state;
	private final LineReader lineReader;
	private String nextLine;

	public LineReaderIterator(State state, LineReader lineReader) {
		this.state = state;
		this.lineReader = lineReader;
	}

	@Override
	public boolean hasNext() {
		if (nextLine == null) {
			try {
				nextLine = lineReader.readLine(computePrompt());
			} catch (EndOfFileException e) {
				nextLine = null;
				return false;
			} catch (UserInterruptException e) {
				nextLine = "";
				return true;
			} catch (IOError e) {
				// this happens when the user hits ctrl-c before Jline sets the signal handler
				if (e.getCause() instanceof java.io.InterruptedIOException) {
					nextLine = "";
					return true;
				} else {
					throw e;
				}
			}
		}
		return true;
	}

	@Override
	public String next() {
		if (nextLine == null) {
			throw new NoSuchElementException();
		} else {
			String line = nextLine;
			nextLine = null;
			return line;
		}
	}

	private String computePrompt() {
		return new AttributedStringBuilder()
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
				.append("hosh:")
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
				.append(String.valueOf(state.getId()))
				.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
				.append("> ")
				.style(AttributedStyle.DEFAULT)
				.toAnsi(lineReader.getTerminal());
	}
}
