package org.hosh.runtime;

import java.util.Iterator;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;

public class LineReaderIterator implements Iterator<String> {

	// TODO: must read the current prompt from the global state
	private final String prompt = "hosh> ";
	private final LineReader lineReader;
	private String nextLine;
	
	public LineReaderIterator(LineReader lineReader) {
		this.lineReader = lineReader;
	}

	@Override
	public boolean hasNext() {
		if (nextLine == null) {
			try {			
				nextLine = lineReader.readLine(prompt);
			} catch (EndOfFileException e) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String next() {
		String line = nextLine;
		nextLine = null;
		return line;
	}
	
}
