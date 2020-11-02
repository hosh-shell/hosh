package hosh.modules.system;


import hosh.spi.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal helper, could be promoted later to be part of hosh-spi and Values.ofDuration.
 */
public class DurationParsing {

	/**
	 * Parsing duration ISO 8601 format with possibility to omit leading 'PT' prefix.
	 *
	 * Any invalid input, including null, returns empty.
	 */
	public static Optional<Duration> parse(String value) {
		try {
			Duration parsed;
			if (value == null) {
				parsed = null;
			} else if (value.startsWith("PT")) {
				parsed = Duration.parse(value);
			} else {
				parsed = Duration.parse("PT" + value);
			}
			return Optional.ofNullable(parsed);
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	private DurationParsing() {
		// to keep sonar happy :-)
	}

}
