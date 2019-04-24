/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package org.hosh.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;

import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class VariableExpansionCompleterTest {

	@Mock(stubOnly = true)
	private LineReader lineReader;

	@Mock(stubOnly = true)
	private ParsedLine parsedLine;

	@Spy
	private State state = new State();

	@Mock
	private List<Candidate> candidates;

	@InjectMocks
	private VariableExpansionCompleter sut;

	@Test
	public void notInExpansion() {
		given(parsedLine.word()).willReturn("a");
		sut.complete(lineReader, parsedLine, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void inExpansionMatchingSingle() {
		given(parsedLine.word()).willReturn("${");
		given(state.getVariables()).willReturn(Map.of("FOO", "whatever"));
		sut.complete(lineReader, parsedLine, candidates);
		then(candidates).should().add(DebuggableCandidate.complete("${FOO}"));
	}

	@Test
	public void inExpansionMatchingMultiple() {
		given(parsedLine.word()).willReturn("${");
		given(state.getVariables()).willReturn(Map.of("FOO", "whatever", "BAR", "whatever"));
		sut.complete(lineReader, parsedLine, candidates);
		then(candidates).should().add(DebuggableCandidate.complete("${FOO}"));
		then(candidates).should().add(DebuggableCandidate.complete("${BAR}"));
	}
}
