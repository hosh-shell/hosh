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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

	@InjectMocks
	private FileSystemCompleter sut;

	@Test
	public void emptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.toPath());
		given(line.word()).willReturn("");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	public void nonEmptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.toPath());
		given(line.word()).willReturn("aaa");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	public void emptyWordInNonEmptyDir() throws IOException {
		temporaryFolder.newFile("a");
		given(state.getCwd()).willReturn(temporaryFolder.toPath());
		given(line.word()).willReturn("");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
				.hasSize(1)
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isEqualTo("a");
					assertThat(candidate.complete()).isTrue();
				});
	}

	@Test
	@Todo(description = "fails on windows, / should be C:/, use Path.getRoot?")
	@DisabledOnOs(OS.WINDOWS)
	public void slash() {
		given(line.word()).willReturn("/");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
				.isNotEmpty()
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isNotBlank();
				});
	}

	@Test
	public void absoluteDirWithoutEndingSeparator() throws IOException {
		File dir = temporaryFolder.newFolder("dir");
		File newFile = temporaryFolder.newFile(dir, "aaa");
		given(line.word()).willReturn(newFile.getParent());
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
				.hasSize(1)
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isEqualTo(dir.getAbsolutePath() + File.separator);
					assertThat(candidate.complete()).isFalse();
				});
	}

	@Test
	public void absoluteDirWithEndingSeparator() throws IOException {
		File dir = temporaryFolder.newFolder("dir");
		File newFile = temporaryFolder.newFile(dir, "aaa");
		given(line.word()).willReturn(newFile.getParent() + File.separator);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
				.hasSize(1)
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isEqualTo(newFile.getAbsolutePath());
					assertThat(candidate.complete()).isTrue();
				});
	}

	@Test
	public void partialMatchDirectory() throws IOException {
		temporaryFolder.newFolder(temporaryFolder.newFolder("aaa"), "bbb");
		given(state.getCwd()).willReturn(temporaryFolder.toPath());
		given(line.word()).willReturn("aaa" + File.separator + "b");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
				.hasSize(1)
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isEqualTo("aaa" + File.separator + "bbb");
					assertThat(candidate.complete()).isTrue();
				});
	}
}
