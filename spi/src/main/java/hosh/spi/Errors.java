package hosh.spi;

import hosh.doc.Experimental;
import hosh.doc.Todo;

import java.util.Objects;

/**
 * Factory methods for common errors.
 */
@Experimental(description = "")
public class Errors {

	/**
	 * Extract message from exception. If not present use "(no message)".
	 */
	public static Record message(Exception e) {
		return Records.singleton(Keys.ERROR, Values.ofText(Objects.toString(e.getMessage(), "(no message)")));
	}

	public static Record message(String fmt, Object... args) {
		return Records.singleton(Keys.ERROR, Values.ofText(String.format(fmt, args)));
	}

	@Todo(description = "adding more details (e.g. file or directory, help, etc)")
	public static Record usage(String usage) {
		return Records.singleton(Keys.ERROR, Values.ofText("usage: " + usage));
	}

	private Errors() {
	}

}
