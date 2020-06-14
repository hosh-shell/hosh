package hosh.spi;

import hosh.doc.Experimental;

/**
 * Factory methods for common errors.
 */
@Experimental(description = "")
public class Errors {

	private Errors() {
	}

	public static Record message(String fmt, Object... args) {
		return Records.singleton(Keys.ERROR, Values.ofText(String.format(fmt, args)));
	}

	public static Record usage(String usage) {
		return Records.singleton(Keys.ERROR, Values.ofText("usage: " + usage));
	}

}
