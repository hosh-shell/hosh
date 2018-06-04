package org.hosh.runtime;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

import org.hosh.spi.State;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;

public class LineReaderIterator implements Iterator<String> {

	private final State state;
	private final LineReader lineReader;
	private String nextLine;

	public LineReaderIterator(@Nonnull State state, @Nonnull LineReader lineReader) {
		this.state = state;
		this.lineReader = lineReader;
	}

	@Override
	public boolean hasNext() {
		if (nextLine == null) {
			try {
				nextLine = lineReader.readLine(state.getPrompt());
			} catch (EndOfFileException e) {
				nextLine = null;
				return false;
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

}
