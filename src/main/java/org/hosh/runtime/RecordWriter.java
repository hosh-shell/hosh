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
