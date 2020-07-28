/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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

import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HoshHighlighterTest {

	@Mock(stubOnly = true)
	Compiler compiler;

	@Mock(stubOnly = true)
	LineReader lineReader;

	@Mock(stubOnly = true)
	Compiler.Program program;

	HoshHighlighter sut;

	@BeforeEach
	void setUp() {
		sut = new HoshHighlighter(compiler);
	}

	@Test
	void noError() {
		given(compiler.compile(any())).willReturn(program);
		AttributedString attributedString = sut.highlight(lineReader, "current line");
		assertThat(attributedString.styleAt(0)).isEqualTo(AttributedStyle.DEFAULT);
	}

	@Test
	void error() {
		given(compiler.compile(any())).willThrow(new RuntimeException("simulated error"));
		AttributedString attributedString = sut.highlight(lineReader, "current line");
		assertThat(attributedString.styleAt(0)).isNotEqualTo(AttributedStyle.DEFAULT); // it is ok to assert that is not default style
	}

}