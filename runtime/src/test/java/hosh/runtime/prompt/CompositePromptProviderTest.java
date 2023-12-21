/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
