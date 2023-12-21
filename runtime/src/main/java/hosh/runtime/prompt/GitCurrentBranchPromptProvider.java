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

import hosh.doc.Todo;
import hosh.spi.Ansi;
import hosh.spi.LoggerFactory;
import hosh.spi.State;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample implementation that just shows the current branch.
 */
public class GitCurrentBranchPromptProvider implements PromptProvider {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	// allows for unit testing since it is not always possible to use git on every build machine
	public interface GitExternal {
		Process create(State state) throws IOException;

		@SuppressWarnings("squid:S4036")
		static GitExternal defaultImplementation() {
			// sonar is reporting a security vulnerability here:
			// the command below is not changing the filesystem or running other "write operations"
			return state -> new ProcessBuilder()
					.command(List.of("git", "branch", "--show-current"))
					.directory(state.getCwd().toFile())
					.start();
		}

	}

	private GitExternal gitExternal = GitExternal.defaultImplementation();

	public void setGitExternal(GitExternal gitExternal) {
		this.gitExternal = gitExternal;
	}

	@Todo(description = "use StyledPrompt class")
	@Override
	public String provide(State state) {
		String currentBranch = getCurrentBranch(state);
		if (currentBranch == null) {
			return null; // nothing to do
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.append("[");
		Ansi.Style.FG_RED.enable(pw);
		pw.append("git:").append(currentBranch);
		Ansi.Style.FG_RED.disable(pw);
		pw.append("]");
		Ansi.Style.RESET.enable(pw);
		return sw.toString();
	}

	private String getCurrentBranch(State state) {
		try {
			Process git = gitExternal.create(state);
			try (BufferedReader bufferedReader = git.inputReader(StandardCharsets.UTF_8)) {
				return bufferedReader.readLine();
			} finally {
				git.destroy();
			}
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "ignoring exception", e);
			return null;
		}
	}
}
