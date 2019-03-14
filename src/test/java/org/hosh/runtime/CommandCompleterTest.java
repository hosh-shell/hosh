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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CommandCompleterTest {
	@Mock(stubOnly = true)
	private State state;
	@Mock(stubOnly = true)
	private Command command;
	@Mock
	private LineReader lineReader;
	@Mock
	private ParsedLine parsedLine;
	@InjectMocks
	private CommandCompleter sut;

	@Test
	public void emptyCommands() {
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		then(lineReader).shouldHaveNoMoreInteractions();
		then(parsedLine).shouldHaveNoMoreInteractions();
		assertThat(candidates).isEmpty();
	}

	@Test
	public void oneCommand() {
		List<Candidate> candidates = new ArrayList<>();
		given(state.getCommands()).willReturn(Collections.singletonMap("cmd", command.getClass()));
		sut.complete(lineReader, parsedLine, candidates);
		then(lineReader).shouldHaveNoMoreInteractions();
		then(parsedLine).shouldHaveNoMoreInteractions();
		assertThat(candidates)
				.isNotEmpty()
				.extracting(Candidate::value)
				.allMatch(p -> p.equals("cmd"));
	}
}
