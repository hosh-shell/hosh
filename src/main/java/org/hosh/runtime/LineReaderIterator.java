/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.hosh.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hosh.doc.Todo;
import org.hosh.spi.Ansi;
import org.hosh.spi.State;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

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

	@Todo(description = "this should be user-configurable")
	private String computePrompt() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		Ansi.Style.FG_BLUE.enable(pw);
		pw.append("hosh:");
		Ansi.Style.FG_CYAN.enable(pw);
		pw.append(String.valueOf(state.getId()));
		Ansi.Style.FG_BLUE.enable(pw);
		pw.append("> ");
		Ansi.Style.RESET.enable(pw);
		return sw.toString();
	}
}
