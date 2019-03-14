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
package org.hosh.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hosh.doc.Bug;
import org.hosh.testsupport.IgnoreIf;
import org.hosh.testsupport.IgnoreIf.IgnoredIf;
import org.hosh.testsupport.IgnoreIf.OnWindows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class HoshIT {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public final IgnoreIf ignoreIf = new IgnoreIf();
	@Rule
	public final TestName testName = new TestName();

	@Before
	public void showTest() {
		System.out.println("  " + testName.getMethodName());
	}

	@Test
	public void interactiveEndOfFile() throws Exception {
		Process hosh = givenHoshProcess();
		closeInput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void interactiveExitSuccess() throws Exception {
		Process hosh = givenHoshProcess();
		sendInput(hosh, "exit");
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void interactiveExitFailure() throws Exception {
		Process hosh = givenHoshProcess();
		sendInput(hosh, "exit 42");
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(42);
	}

	@Test
	public void wrapperThenPipeline() throws Exception {
		Path scriptPath = givenScript(
				"withTime { cwd } | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).isEqualToNormalizingNewlines("2");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void scriptWithCdAndCwd() throws Exception {
		Path scriptPath = givenScript(
				"cd " + temporaryFolder.getRoot().getAbsolutePath(),
				"cwd"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains(temporaryFolder.getRoot().getAbsolutePath());
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void scriptWithOsVar() throws Exception {
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE}"//
		);
		Process hosh = givenHoshProcess(Collections.singletonMap("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("hello world!");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void scriptWithMissingOsVar() throws Exception {
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE}"//
		);
		Process hosh = givenHoshProcess(Collections.emptyMap(), scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("unknown variable: OS_ENV_VARIABLE");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void scriptWithUnknownCommand() throws Exception {
		Path scriptPath = givenScript(
				"AAAAB"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("line 1: 'AAAAB' unknown command");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void scriptWithImplicitExit() throws Exception {
		Path scriptPath = givenScript(
				"echo hello"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).isEqualTo("hello");
	}

	@Test
	public void scriptWithExplicitExit() throws Exception {
		Path scriptPath = givenScript(
				"exit 1"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void scriptParsedThenExecuted() throws Exception {
		Path scriptPath = givenScript(
				"exit 0",
				"AAAAB"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).contains("line 2: 'AAAAB' unknown command");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void nonPipelineExternalCommand() throws Exception {
		Path scriptPath = givenScript(
				"java --version "//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).contains("Runtime Environment");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void pipelineWithInternalCommand() throws Exception {
		Path scriptPath = givenScript(
				"source | take 1 | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).isEqualTo("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void pipelineWithExternalCommand() throws Exception {
		Path scriptPath = givenScript(
				"java --version | take 1 | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).isEqualTo("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@IgnoredIf(description = "windows does not have 'wc'", condition = OnWindows.class)
	@Test
	public void pipelineWriteExternalCommand() throws Exception {
		Path scriptPath = givenScript(
				"cwd | wc -l"// 'cwd' is a Java command that writes into 'wc -l' (native command)
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).isEqualToNormalizingWhitespace("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void missingScript() throws Exception {
		Path scriptPath = Paths.get("missing.hosh");
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("unable to load: missing.hosh");
		assertThat(exitCode).isEqualTo(1);
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/23")
	@Test
	public void pipelinesDontExpandVariables() throws Exception {
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE} | take 1"//
		);
		Process hosh = givenHoshProcess(Collections.singletonMap("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).isEqualTo("hello world!");
		assertThat(exitCode).isEqualTo(0);
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/23")
	@Test
	public void wrappersDontExpandVariables() throws Exception {
		Path scriptPath = givenScript(
				"withTime { echo ${OS_ENV_VARIABLE} } "//
		);
		Process hosh = givenHoshProcess(Collections.singletonMap("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("hello world!");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void errorInProducer() throws Exception {
		Path scriptPath = givenScript(
				"err | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("injected error: please do not report");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void errorInConsumer() throws Exception {
		Path scriptPath = givenScript(
				"ls | err"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("injected error: please do not report");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void consumeInfiniteProducer() throws Exception {
		Path scriptPath = givenScript(
				"rand | take 100 | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).isEqualTo("100");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void benchmark() throws Exception {
		Path scriptPath = givenScript(
				"benchmark 10 { rand | take 100 | count } "//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).startsWith("10");
	}

	@Test
	public void unknownCommandScript() throws Exception {
		Path scriptPath = givenScript(
				"FOOBAR"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(1);
		assertThat(output).contains("line 1: 'FOOBAR' unknown command");
	}

	@Test
	public void unknownCommandInteractive() throws Exception {
		Process hosh = givenHoshProcess();
		sendInput(hosh, "FOOBAR\nexit\n");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).contains("'FOOBAR' unknown command");
	}

	// simple test infrastructure
	private Path givenScript(String... lines) throws IOException {
		Path scriptPath = temporaryFolder.newFile("test.hosh").toPath();
		Files.write(scriptPath, Arrays.asList(lines));
		return scriptPath;
	}

	private Process givenHoshProcess(String... args) throws IOException {
		return givenHoshProcess(Collections.emptyMap(), args);
	}

	private Process givenHoshProcess(Map<String, String> env, String... args) throws IOException {
		List<String> cmd = new ArrayList<>();
		cmd.add("java");
		ProcessHandle.current().info().arguments().ifPresent(jvmArgs -> {
			if (jvmArgs[0].contains("jacoco")) {
				cmd.add(jvmArgs[0]);
			}
		});
		cmd.addAll(Arrays.asList("-jar", "target/dist/hosh.jar"));
		for (String arg : args) {
			cmd.add(arg);
		}
		ProcessBuilder pb = new ProcessBuilder()
				.command(cmd)
				.redirectErrorStream(true);
		pb.environment().putAll(env);
		return pb.start();
	}

	private String consumeOutput(Process hosh) throws IOException {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(hosh.getInputStream(), StandardCharsets.UTF_8))) {
			return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private void sendInput(Process hosh, String line) throws IOException {
		try (Writer writer = new OutputStreamWriter(hosh.getOutputStream(), StandardCharsets.UTF_8)) {
			writer.write(line);
			writer.write(System.lineSeparator());
			writer.flush();
		}
	}

	// send end of file to the process
	private void closeInput(Process hosh) throws IOException {
		hosh.getOutputStream().close();
	}
}
