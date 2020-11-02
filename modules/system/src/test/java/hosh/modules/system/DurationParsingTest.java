package hosh.modules.system;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DurationParsingTest {

	@Test
	void iso8601Format() {
		Optional<Duration> result = DurationParsing.parseIso("PT5S");
		assertThat(result).contains(Duration.ofSeconds(5));
	}

	@Test
	void ourCustomFormat() {
		Optional<Duration> result = DurationParsing.parseIso("5s");
		assertThat(result).contains(Duration.ofSeconds(5));
	}
}