package hosh.modules.system;


import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class DurationParsing {

	private DurationParsing() {
		// to keep sonar happy :-)
	}

	public static Optional<Duration> parseIso(String value) {
		try {
			Duration parsed;
			if (value.startsWith("PT")) {
				parsed = Duration.parse(value);
			} else {
				parsed = Duration.parse("PT" + value);
			}
			return Optional.of(parsed);
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

}
