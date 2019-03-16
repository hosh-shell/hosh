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
import java.util.Locale;

import org.hosh.doc.Todo;
import org.hosh.spi.Ansi;
import org.hosh.spi.Record;

@Todo(description = "perhaps this should be moved into the spi package?")
public class RecordWriter {
	private final PrintWriter pw;
	private final Ansi.Style style;

	public RecordWriter(PrintWriter pw, Ansi.Style style) {
		this.pw = pw;
		this.style = style;
	}

	public void writeValues(Record record) {
		Locale locale = Locale.getDefault();
		style.enable(pw);
		var iterator = record.values().iterator();
		while (iterator.hasNext()) {
			var value = iterator.next();
			value.append(pw, locale);
			if (iterator.hasNext()) {
				pw.append(" ");
			}
		}
		style.disable(pw);
		pw.append(System.lineSeparator());
		pw.flush();
	}
}
