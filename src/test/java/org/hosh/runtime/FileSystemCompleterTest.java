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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hosh.doc.Todo;
import org.hosh.spi.State;
import org.hosh.testsupport.TemporaryFolder;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileSystemCompleterTest {

	@RegisterExtension
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Mock(stubOnly = true)
	private State state;

	@Mock(stubOnly = true)
	private LineReader lineReader;

	@Mock(stubOnly = true)
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
		given(line.word()).willReturn("");
		sut.complete(lineReader, line, candidates);
		then(candidates).should().add(DebuggableCandidate.complete("a"));
	}

	@Test
	@Todo(description = "fails on windows, / should be C:/, use Path.getRoot?")
	@DisabledOnOs(OS.WINDOWS)
	public void slash() {
		given(line.word()).willReturn("/");
		sut.complete(lineReader, line, candidates);
		then(candidates).should(Mockito.atLeastOnce()).add(Mockito.any());
	}

	@Test
	public void absoluteDirWithoutEndingSeparator() throws IOException {
		File newFile = temporaryFolder.newFile("aaa");
		given(line.word()).willReturn(newFile.getParent());
		sut.complete(lineReader, line, candidates);
		then(candidates).should().add(DebuggableCandidate.incomplete(temporaryFolder.getRoot().getAbsolutePath() + File.separator));
	}

	@Test
	public void absoluteDirWithEndingSeparator() throws IOException {
		File newFile = temporaryFolder.newFile("aaa");
		given(line.word()).willReturn(newFile.getParent() + File.separator);
		sut.complete(lineReader, line, candidates);
		then(candidates).should().add(DebuggableCandidate.complete(newFile.getAbsolutePath()));
	}

	@Test
	public void partialMatchDirectory() throws IOException {
		given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
		temporaryFolder.newFolder("aaa", "bbb");
		given(line.word()).willReturn("aaa" + File.separator + "b");
		sut.complete(lineReader, line, candidates);
		then(candidates).should(Mockito.atLeastOnce()).add(DebuggableCandidate.complete("aaa" + File.separator + "bbb"));
	}
}
