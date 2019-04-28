package org.hosh.spi;

import java.io.PrintWriter;
import java.util.Locale;

import org.hosh.doc.Todo;

@Todo(description = "add record_separator (hardcoded to ' ') and boolean newline, perhaps in a nice fluent interface")
public interface Printable {

	void print(PrintWriter printWriter, Locale locale);
}
