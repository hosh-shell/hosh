package hosh.runtime.prompt;

import hosh.spi.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CompositePromptProviderTest {

	@Mock(stubOnly = true)
	State state;

	@Mock(stubOnly = true)
	PromptProvider promptProvider1;

	@Mock(stubOnly = true)
	PromptProvider promptProvider2;

	@Test
	void empty() {
		// Given
		CompositePromptProvider sut = new CompositePromptProvider(
				List.of(
						// empty
				)
		);

		// When
		sut.provide(state);
		// Then
	}

	@Test
	void one() {
		// Given
		given(promptProvider1.provide(state)).willReturn("1");
		CompositePromptProvider sut = new CompositePromptProvider(
				List.of(
						promptProvider1
				)
		);

		// When
		String result = sut.provide(state);

		// Then
		assertThat(result).isEqualTo("1");
	}

	@Test
	void two() {
		// Given
		given(promptProvider1.provide(state)).willReturn("1");
		given(promptProvider2.provide(state)).willReturn("2");
		CompositePromptProvider sut = new CompositePromptProvider(
				List.of(
						promptProvider1,
						promptProvider2
				)
		);

		// When
		String result = sut.provide(state);

		// Then
		assertThat(result).isEqualTo("12");
	}

	@Test
	void oneMissingPrompt() {
		// Given
		given(promptProvider1.provide(state)).willReturn(null); // will be skipped
		given(promptProvider2.provide(state)).willReturn("2");
		CompositePromptProvider sut = new CompositePromptProvider(
				List.of(
						promptProvider1,
						promptProvider2
				)
		);

		// When
		String result = sut.provide(state);

		// Then
		assertThat(result).isEqualTo("2");
	}

	@Test
	void allMissingPrompts() {
		// Given
		given(promptProvider1.provide(state)).willReturn(null); // will be skipped
		given(promptProvider2.provide(state)).willReturn(null); // will be skipped
		CompositePromptProvider sut = new CompositePromptProvider(
				List.of(
						promptProvider1,
						promptProvider2
				)
		);

		// When
		String result = sut.provide(state);

		// Then
		assertThat(result).isEqualTo(null);
	}
}
