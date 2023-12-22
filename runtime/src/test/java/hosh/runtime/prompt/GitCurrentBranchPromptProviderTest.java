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
package hosh.runtime.prompt;

import hosh.spi.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GitCurrentBranchPromptProviderTest {

	@Mock(stubOnly = true)
	State state;

	@Mock(stubOnly = true)
	GitCurrentBranchPromptProvider.GitExternal gitExternal;

	@Mock
	Process process;

	@Test
	void notWorkingGit() throws IOException {
		// Given
		given(gitExternal.create(state)).willThrow(new IOException("simulated exception for 'no git'"));
		GitCurrentBranchPromptProvider sut = new GitCurrentBranchPromptProvider();
		sut.setGitExternal(gitExternal);

		// When
		String result = sut.provide(state);

		// Then
		assertThat(result).isNull();
	}

	@Test
	void workingGit() throws IOException {
		// Given
		given(gitExternal.create(state)).willReturn(process);
		given(process.inputReader(StandardCharsets.UTF_8)).willReturn(new BufferedReader(new StringReader("bugfix/npe-main")));
		GitCurrentBranchPromptProvider sut = new GitCurrentBranchPromptProvider();
		sut.setGitExternal(gitExternal);

		// When
		String result = sut.provide(state);

		// Then
		then(process).should().destroy(); // important to not leak process handlers
		assertThat(result).isEqualTo("[\u001B[31mgit:bugfix/npe-main\u001B[39m]");
	}

}