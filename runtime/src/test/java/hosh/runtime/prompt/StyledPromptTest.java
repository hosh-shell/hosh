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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StyledPromptTest {

	@Test
	void noStyle() {
		// Given
		StyledPrompt sut = new StyledPrompt();

		// When
		String result = sut.render("hosh> ");

		// Then
		assertThat(result).isEqualTo("hosh> ");
	}

	@Test
	void oneStyle() {
		// Given
		StyledPrompt sut = new StyledPrompt();

		// When
		String result = sut.render("@{fg:green user}> ");

		// Then
		assertThat(result).isEqualTo("\u001B[32muser\u001B[0m> ");
	}

	@Test
	void twoStyles() {
		// Given
		StyledPrompt sut = new StyledPrompt();

		// When
		String result = sut.render("@{fg:green user}@@{fg:red server}> ");

		// Then
		assertThat(result).isEqualTo("\u001B[32muser\u001B[0m@\u001B[31mserver\u001B[0m> ");
	}

}