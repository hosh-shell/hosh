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

import hosh.spi.Command;
import hosh.spi.State;
import hosh.test.support.TemporaryFolder;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommandCompleterTest {

	@RegisterExtension
	final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Mock(stubOnly = true)
	Command command;

	@Mock(stubOnly = true)
	State state;

	@Mock(stubOnly = true)
	LineReader lineReader;

	@Mock(stubOnly = true)
	ParsedLine parsedLine;

	CommandCompleter sut;

	@BeforeEach
	void createSut() {
		sut = new CommandCompleter(state);
	}

	@Test
	void emptyPathAndNoBuiltins() {
		given(state.getPath()).willReturn(List.of());
		given(parsedLine.wordIndex()).willReturn(0);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	void builtin() {
		given(state.getCommands()).willReturn(Map.of("cmd", () -> command));
		given(parsedLine.wordIndex()).willReturn(0);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates)
				.hasSize(1)
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isEqualTo("cmd");
					assertThat(candidate.descr()).isEqualTo("built-in");
				});
	}

	@Test
	void builtinOverridesExternal() throws IOException {
		given(state.getPath()).willReturn(List.of(temporaryFolder.toPath()));
		given(parsedLine.wordIndex()).willReturn(0);
		Path file = temporaryFolder.newFile(temporaryFolder.toPath(), "cmd");
		assertThat(file.toFile().setExecutable(true, true)).isTrue();
		given(state.getCommands()).willReturn(Map.of("cmd", () -> command));
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates)
				.hasSize(1)
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isEqualTo("cmd");
					assertThat(candidate.descr()).isEqualTo("built-in, overrides " + file.toAbsolutePath());
				});
	}

	@Test
	void pathWithEmptyDir() {
		given(state.getPath()).willReturn(List.of(temporaryFolder.toPath()));
		given(parsedLine.wordIndex()).willReturn(0);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	void pathWithExecutable() throws IOException {
		given(state.getPath()).willReturn(List.of(temporaryFolder.toPath()));
		given(parsedLine.wordIndex()).willReturn(0);
		Path file = temporaryFolder.newFile(temporaryFolder.toPath(), "cmd");
		assertThat(file.toFile().setExecutable(true, true)).isTrue();
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates)
				.hasSize(1)
				.allSatisfy(candidate -> {
					assertThat(candidate.value()).isEqualTo("cmd");
					assertThat(candidate.descr()).isEqualTo("external in " + temporaryFolder.toPath().toAbsolutePath());
				});
	}

	@Test
	void pathWithExecutableAndNonEmptyLine() throws IOException {
		given(parsedLine.wordIndex()).willReturn(1);  // skipping autocomplete
		Path file = temporaryFolder.newFile(temporaryFolder.toPath(), "cmd");
		assertThat(file.toFile().setExecutable(true, true)).isTrue();
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	void skipNonInPathDirectory() throws IOException {
		Path file = temporaryFolder.newFile(temporaryFolder.toPath(), "aaa");
		given(state.getPath()).willReturn(List.of(file));
		given(parsedLine.wordIndex()).willReturn(0);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates).isEmpty();
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void ioErrorsAreIgnored() throws IOException {
		Path bin = temporaryFolder.newFolder("bin");
		assertTrue(bin.toFile().setReadable(false)); // throws java.nio.file.AccessDeniedException, does not work on windows
		given(state.getPath()).willReturn(List.of(bin.toAbsolutePath()));
		given(parsedLine.wordIndex()).willReturn(0);
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		assertThat(candidates).isEmpty();
	}
}
