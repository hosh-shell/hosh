/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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

import java.io.IOException;
import java.util.List;

import org.hosh.spi.State;
import org.hosh.testsupport.IgnoreIf;
import org.hosh.testsupport.IgnoreIf.IgnoredIf;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class FileSystemCompleterTest {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public final IgnoreIf ignoreIf = new IgnoreIf();
	@Mock
	private State state;
	@Mock(stubOnly = true)
	private LineReader lineReader;
	@Mock
	private ParsedLine line;
	@Mock
	private List<Candidate> candidates;
	@InjectMocks
	private FileSystemCompleter sut;

	@Test
	public void emptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
		given(line.word()).willReturn("");
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void nonEmptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
		given(line.word()).willReturn("aaa");
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void emptyWordInNonEmptyDir() throws IOException {
		temporaryFolder.newFile("a");
		given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
		given(line.word()).willReturn("aaa");
		sut.complete(lineReader, line, candidates);
		then(candidates).should().add(ArgumentMatchers.any());
	}

	@Test
	@IgnoredIf(description = "fails on windows, / should be C:/", condition = IgnoreIf.OnWindows.class)
	public void slash() {
		given(line.word()).willReturn("/");
		sut.complete(lineReader, line, candidates);
		then(candidates).should(Mockito.atLeastOnce()).add(ArgumentMatchers.any());
	}

	@Test
	public void absoluteDir() throws IOException {
		String dir = temporaryFolder.getRoot().getAbsolutePath();
		temporaryFolder.newFile();
		given(line.word()).willReturn(dir);
		sut.complete(lineReader, line, candidates);
		then(candidates).should(Mockito.atLeastOnce()).add(ArgumentMatchers.any());
	}
}
