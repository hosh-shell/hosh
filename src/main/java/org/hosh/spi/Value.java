package org.hosh.spi;

import java.util.Locale;

/**
 * Any value produced by commands such as text, size or path.
 *
 * Values are immutable.
 */
public interface Value {
	void append(Appendable appendable, Locale locale);
}
