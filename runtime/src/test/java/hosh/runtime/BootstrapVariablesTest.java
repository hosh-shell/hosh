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
package hosh.runtime;

import hosh.spi.VariableName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapVariablesTest {

	@Test
	void empty() {
		// Given
		Map<String, String> input = Map.of();

		// When
		Map<VariableName, String> result = new BootstrapVariables().fromEnv(input);

		// Then
		assertThat(result)
				.isEmpty()
		;
	}

	@Test
	void oneVariableValid() {
		// Given
		Map<String, String> input = Map.of("FOO", "value");

		// When
		Map<VariableName, String> result = new BootstrapVariables().fromEnv(input);

		// Then
		assertThat(result)
				.hasSize(1)
				.containsEntry(VariableName.constant("FOO"), "value")
				;
	}

	@Test
	void oneVariableInvalid() {
		// Given
		Map<String, String> input = Map.of("123", "value");

		// When
		Map<VariableName, String> result = new BootstrapVariables().fromEnv(input);

		// Then
		assertThat(result)
				.isEmpty() // 123 is not representable as VariableName
		;
	}

	@Test
	void currentEnv() {
		// Given
		Map<String, String> input = System.getenv();

		// When
		Map<VariableName, String> result = new BootstrapVariables().fromEnv(input);

		// Then
		assertThat(result)
				.isNotEmpty() // while the assert is pretty weak, there is *some* value in running this test often
		;
	}

}