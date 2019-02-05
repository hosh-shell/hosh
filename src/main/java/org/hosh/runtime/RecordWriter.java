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
	private String prefix;
	private String suffix;

	public RecordWriter(Appendable appendable) {
		this.appendable = appendable;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
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
		if (prefix != null) {
			appendable.append(prefix);
		}
		while (values.hasNext()) {
			Value value = values.next();
			value.append(appendable, locale);
			if (values.hasNext()) {
				appendable.append(" ");
			}
		}
		if (suffix != null) {
			appendable.append(suffix);
		}
		appendable.append(System.lineSeparator());
	}
}
