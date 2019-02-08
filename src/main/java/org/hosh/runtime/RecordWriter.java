/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Locale;

import org.hosh.doc.Experimental;
import org.hosh.doc.Todo;
import org.hosh.spi.Record;
import org.hosh.spi.Value;

@Todo(description = "users cannot change separator by now")
@Experimental(description = "standard way to writer record as string")
public class RecordWriter {
	private final Appendable appendable;
	private final Ansi.Style style;

	public RecordWriter(Appendable appendable, Ansi.Style style) {
		this.appendable = appendable;
		this.style = style;
	}

	public void writeValues(Record record) {
		try {
			tryWriteValues(record);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void tryWriteValues(Record record) throws IOException {
		Locale locale = Locale.getDefault();
		Iterator<Value> values = record.values().iterator();
		style.enable(appendable);
		while (values.hasNext()) {
			Value value = values.next();
			value.append(appendable, locale);
			if (values.hasNext()) {
				appendable.append(" ");
			}
		}
		style.disable(appendable);
		appendable.append(System.lineSeparator());
	}
}
