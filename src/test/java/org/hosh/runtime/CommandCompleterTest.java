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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.hosh.testsupport.TemporaryFolder;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandCompleterTest {

	@RegisterExtension
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Mock(stubOnly = true)
	private Command command;

	@Mock(stubOnly = true)
	private State state;

	@Mock(stubOnly = true)
	private LineReader lineReader;

	@Mock(stubOnly = true)
	private ParsedLine line;

	@Mock
	private List<Candidate> candidates;

	@InjectMocks
	private CommandCompleter sut;

	@Test
	public void emptyPathAndNoBuitins() {
		given(state.getPath()).willReturn(List.of());
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void builtin() {
		List<Candidate> candidates = new ArrayList<>();
		given(state.getCommands()).willReturn(Collections.singletonMap("cmd", command.getClass()));
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
				.containsExactly(DebuggableCandidate.completeWithDescription("cmd", "built-in"));
	}

	@Test
	public void builtinOverridesExternal() throws IOException {
		given(state.getPath()).willReturn(List.of(temporaryFolder.toPath()));
		File file = temporaryFolder.newFile("cmd");
		assert file.setExecutable(true, true);
		List<Candidate> candidates = new ArrayList<>();
		given(state.getCommands()).willReturn(Collections.singletonMap("cmd", command.getClass()));
		sut.complete(lineReader, line, candidates);
		assertThat(candidates)
				.hasSize(1)
				.first()
				.satisfies(candidate -> {
					assertThat(candidate.descr()).isEqualTo("built-in, overrides " + file.getAbsolutePath());
				});
	}

	@Test
	public void pathWithEmptyDir() {
		given(state.getPath()).willReturn(List.of(temporaryFolder.toPath()));
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void pathWithExecutable() throws IOException {
		given(state.getPath()).willReturn(List.of(temporaryFolder.toPath()));
		File file = temporaryFolder.newFile("executable");
		assert file.setExecutable(true, true);
		sut.complete(lineReader, line, candidates);
		then(candidates).should()
				.add(DebuggableCandidate.completeWithDescription("executable", "external " + temporaryFolder.toFile().getAbsolutePath()));
	}

	@Test
	public void skipNonInPathDirectory() throws IOException {
		File file = temporaryFolder.newFile();
		given(state.getPath()).willReturn(List.of(file.toPath()));
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void ioErrorsAreIgnored() throws IOException {
		File bin = temporaryFolder.newFolder("bin");
		bin.setExecutable(false);
		bin.setReadable(false); // throws java.nio.file.AccessDeniedException
		given(state.getPath()).willReturn(List.of(bin.toPath().toAbsolutePath()));
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}
}
