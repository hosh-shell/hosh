package org.hosh.spi;

import java.util.Locale;

import org.hosh.doc.Experimental;

/**
 * Any value produced by commands such as text, size or path.
 *
 * Values are immutable.
 */
public interface Value extends Comparable<Value> {
	void append(Appendable appendable, Locale locale);

	@Experimental(description = "not sure about this design, maybe is better to pass another Value-type object?")
	default boolean matches(@SuppressWarnings("unused") Object obj) {
		return false;
	}
}
