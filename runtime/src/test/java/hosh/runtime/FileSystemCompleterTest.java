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

import hosh.spi.State;
import hosh.test.support.TemporaryFolder;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FileSystemCompleterTest {

	@RegisterExtension
	final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Mock(stubOnly = true)
	State state;

	@Mock(stubOnly = true)
	LineReader lineReader;

	@Mock(stubOnly = true)
	ParsedLine line;

	FileSystemCompleter sut;

	@BeforeEach
	void createSut() {
		sut = new FileSystemCompleter(state);
	}

	@Test
	void emptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.toPath());
		given(line.word()).willReturn("");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	void nonEmptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.toPath());
		given(line.word()).willReturn("aaa");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	void emptyWordInNonEmptyDir() throws IOException {
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

	@DisabledOnOs(OS.WINDOWS)
	@Test
	void rootDirUnix() {
		String rootDir = "/";
		given(line.word()).willReturn(rootDir);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
			.isNotEmpty()
			.allSatisfy(candidate -> assertThat(candidate.value()).isNotBlank());
	}

	@EnabledOnOs(OS.WINDOWS)
	@Test
	void rootDirWindows() {
		String rootDir = "c:\\";
		given(line.word()).willReturn(rootDir);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
			.isNotEmpty()
			.allSatisfy(candidate -> assertThat(candidate.value()).isNotBlank());
	}

	@Test
	void absoluteDirWithoutEndingSeparator() throws IOException {
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
	void absoluteDirWithEndingSeparator() throws IOException {
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
	void partialMatchDirectory() throws IOException {
		temporaryFolder.newFolder(temporaryFolder.newFolder("aaa"), "bbb");
		given(state.getCwd()).willReturn(temporaryFolder.toPath());
		given(line.word()).willReturn("aaa" + File.separator + "b");
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
			.hasSize(1)
			.allSatisfy(candidate -> {
				assertThat(candidate.value()).isEqualTo("aaa" + File.separator + "bbb" + File.separator);
				assertThat(candidate.complete()).isFalse();
			});
	}
}
