package hosh.modules.system;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DurationParsingTest {

	@Test
	void iso8601FormatUppercase() {
		Optional<Duration> result = DurationParsing.parseIso("PT5S");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void iso8601FormatLowercase() {
		Optional<Duration> result = DurationParsing.parseIso("PT5s");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void ourCustomFormatLowercase() {
		Optional<Duration> result = DurationParsing.parseIso("5s");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void ourCustomFormatUppercase() {
		Optional<Duration> result = DurationParsing.parseIso("5S");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void nullValue() {
		Optional<Duration> result = DurationParsing.parseIso(null);
		assertThat(result).isEmpty();
	}

	@Test
	void emptyValue() {
		Optional<Duration> result = DurationParsing.parseIso("");
		assertThat(result).isEmpty();
	}

	@Test
	void missingUnit() {
		Optional<Duration> result = DurationParsing.parseIso("5");
		assertThat(result).isEmpty();
	}
}