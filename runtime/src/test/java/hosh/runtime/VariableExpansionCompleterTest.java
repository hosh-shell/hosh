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

import hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VariableExpansionCompleterTest {

	@Mock(stubOnly = true)
	LineReader lineReader;

	@Mock(stubOnly = true)
	ParsedLine parsedLine;

	@Spy
	final State state = new State();

	@InjectMocks
	VariableExpansionCompleter sut;

	@Test
	void notInExpansion() {
		given(parsedLine.word()).willReturn("a");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	void inExpansionMatchingSingle() {
		given(parsedLine.word()).willReturn("${");
		given(state.getVariables()).willReturn(Map.of("FOO", "whatever"));
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates)
			.hasSize(1)
			.allSatisfy(candidate -> {
				assertThat(candidate.value()).isEqualTo("${FOO}");
				assertThat(candidate.descr()).isNull();
			});
	}

	@Test
	void inExpansionMatchingMultiple() {
		given(parsedLine.word()).willReturn("${");
		given(state.getVariables()).willReturn(Map.of("FOO", "whatever", "BAR", "whatever"));
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates)
			.hasSize(2)
			.allSatisfy(candidate -> {
				assertThat(candidate.value()).matches("\\Q${FOO}\\E|\\Q${BAR}\\E");
				assertThat(candidate.descr()).isNull();
			});
	}
}
