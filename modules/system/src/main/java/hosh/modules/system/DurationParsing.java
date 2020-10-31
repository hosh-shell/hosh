package hosh.modules.system;


import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

public class DurationParsing {

	private DurationParsing() {
		// to keep sonar happy :-)
	}

	// accept also values without PT later
	public static Optional<Duration> parseIso(String value) {
		try {
			Duration parsed = Duration.parse(value);
			return Optional.of(parsed);
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	public static Optional<Duration> parseValueWithUnit(String value, String unit) {
		Optional<ChronoUnit> chronoUnit = parseUnit(unit);
		OptionalLong chronoValue = parseValue(value);
		if (chronoUnit.isEmpty() || chronoValue.isPresent()) {
			return Optional.empty();
		} else {
			return Optional.of(Duration.of(chronoValue.getAsLong(), chronoUnit.get()));
		}
	}

	private static OptionalLong parseValue(String value) {
		try {
			return OptionalLong.of(Long.parseLong(value));
		} catch (NumberFormatException e) {
			return OptionalLong.empty();
		}
	}

	private static final Map<String, ChronoUnit> VALID_UNITS = Map.ofEntries(
		Map.entry("nanos", ChronoUnit.NANOS),
		Map.entry("micros", ChronoUnit.MICROS),
		Map.entry("millis", ChronoUnit.MILLIS),
		Map.entry("seconds", ChronoUnit.SECONDS),
		Map.entry("minutes", ChronoUnit.MINUTES),
		Map.entry("hours", ChronoUnit.HOURS)
	);

	private static Optional<ChronoUnit> parseUnit(String value) {
		return Optional.ofNullable(VALID_UNITS.get(value));
	}
}
