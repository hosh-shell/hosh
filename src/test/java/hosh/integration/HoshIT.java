/*
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
package hosh.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import hosh.doc.Bug;
import hosh.doc.Todo;
import hosh.testsupport.TemporaryFolder;

public class HoshIT {

	@RegisterExtension
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@AfterEach
	void beforeEach(TestInfo testInfo) {
		System.out.println(testInfo.getDisplayName());
	}

	@Test
	public void interactiveEndOfFile() throws Exception {
		Process hosh = givenHoshProcess();
		closeInput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void interactiveScriptThenExit() throws Exception {
		Process hosh = givenHoshProcess();
		sendInput(hosh, "ls", "exit");
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
	public void scriptWithCdAndCwd() throws Exception {
		Path scriptPath = givenScript(
				"cd " + temporaryFolder.toPath().toAbsolutePath(),
				"cwd"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains(temporaryFolder.toPath().toAbsolutePath().toString());
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void scriptWithOsVar() throws Exception {
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE}"//
		);
		Process hosh = givenHoshProcess(Map.of("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
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
		assertThat(output).contains("cannot resolve variable: OS_ENV_VARIABLE");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void scriptWithMissingOsVarWithFallback() throws Exception {
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE!fallback}"//
		);
		Process hosh = givenHoshProcess(Collections.emptyMap(), scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("fallback");
		assertThat(exitCode).isEqualTo(0);
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
				"git --version"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).startsWith("git version");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void pipelineWithInternalCommand() throws Exception {
		Path scriptPath = givenScript(
				"rand | take 1 | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).isEqualTo("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void pipelineReadFromExternalCommand() throws Exception {
		Path scriptPath = givenScript(
				"git --version | take 1 | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).isEqualTo("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void pipelineWriteToExternalCommand() throws Exception {
		Path scriptPath = givenScript(
				"cwd | git cat-file --batch" //
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).containsOnlyOnce("missing");
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
		Process hosh = givenHoshProcess(Map.of("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
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
		Process hosh = givenHoshProcess(Map.of("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
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
				"benchmark 2 { rand | take 100 | count } "//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).matches("100\r?\n100\r?\n2 PT\\d+.\\d+S PT\\d+.\\d+S PT\\d+.\\d+S");
	}

	@Test
	public void unknownCommandInScript() throws Exception {
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

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/71")
	@Test
	public void commandWrapperCapturesOutput() throws Exception {
		Path scriptPath = givenScript(
				"benchmark 1 { cwd } | schema"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).contains("path", "count best worst average");
	}

	@Test
	public void redirectOutputToVariable() throws Exception {
		Path scriptPath = givenScript(
				"echo 'world' | capture WHO", // this is WHO=$(echo 'world')
				"echo hello ${WHO}"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).contains("hello world");
	}

	@Test
	public void comments() throws Exception {
		Path scriptPath = givenScript(
				"# echo 'hello'",
				"# ls",
				"exit 42"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(42);
		assertThat(output).isEmpty();
	}

	@Test
	public void sequence() throws Exception {
		Path scriptPath = givenScript(
				"echo a ; echo b"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).contains("a", "b");
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/83", description = "inference of ';'")
	@Test
	public void multilinePipeline() throws Exception {
		Path scriptPath = givenScript(
				"rand      |",
				"  take 42 |",
				"  count    ");
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).contains("42");
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	public void interruptBuiltInCommand() throws Exception {
		Path scriptPath = givenScript("rand");
		Process hosh = givenHoshProcess(scriptPath.toString());
		sendSigint(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isNotEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	public void interruptExternalCommand() throws Exception {
		Path scriptPath = givenScript("git cat-file --batch");
		Process hosh = givenHoshProcess(scriptPath.toString());
		sendSigint(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isNotEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	public void interruptPipeline() throws Exception {
		Path scriptPath = givenScript("rand | count");
		Process hosh = givenHoshProcess(scriptPath.toString());
		sendSigint(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isNotEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	public void interruptBenchmark() throws Exception {
		Path scriptPath = givenScript("benchmark 10000 { rand | take 10000 | count }");
		Process hosh = givenHoshProcess(scriptPath.toString());
		sendSigint(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isNotEqualTo(0);
	}

	@Test
	public void versionLongOption() throws Exception {
		Process hosh = givenHoshProcess("--version");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).startsWith("hosh v");
	}

	@Test
	public void versionShortOption() throws Exception {
		Process hosh = givenHoshProcess("-v");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).startsWith("hosh v");
	}

	@Test
	public void helpLongOption() throws Exception {
		Process hosh = givenHoshProcess("--help");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).startsWith("usage: ");
	}

	@Test
	public void helpShortOption() throws Exception {
		Process hosh = givenHoshProcess("-h");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).startsWith("usage: ");
	}

	@Test
	public void invalidOption() throws Exception {
		Process hosh = givenHoshProcess("--blahblah");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(1);
		assertThat(output).startsWith("hosh: Unrecognized option: --blahblah");
	}

	@Test
	public void tooManyArgs() throws Exception {
		Process hosh = givenHoshProcess("aaa.hosh", "bbb.hosh");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(1);
		assertThat(output).startsWith("hosh: too many scripts");
	}

	// simple test infrastructure
	private Path givenScript(String... lines) throws IOException {
		Path scriptPath = temporaryFolder.newFile("test.hosh").toPath();
		Files.write(scriptPath, List.of(lines));
		return scriptPath;
	}

	private Process givenHoshProcess(String... args) throws IOException {
		return givenHoshProcess(Collections.emptyMap(), args);
	}

	private Process givenHoshProcess(Map<String, String> additionalEnv, String... args) throws IOException {
		List<String> cmd = new ArrayList<>();
		cmd.add(absoluteJavaBinary());
		cmd.addAll(propagateJacocoAgentInvocation());
		cmd.addAll(List.of("-jar", "target/dist/hosh.jar"));
		cmd.addAll(List.of(args));
		ProcessBuilder pb = new ProcessBuilder()
				.command(cmd)
				.redirectErrorStream(true);
		pb.environment().putAll(additionalEnv);
		return pb.start();
	}

	private String absoluteJavaBinary() {
		String javaHome = System.getenv("JAVA_HOME");
		if (javaHome == null) {
			return "java";
		}
		return javaHome + File.separator + "bin" + File.separator + "java";
	}

	private List<String> propagateJacocoAgentInvocation() {
		String[] arguments = ProcessHandle
				.current()
				.info()
				.arguments()
				.orElse(new String[0]);
		return Stream.of(arguments)
				.filter(s -> s.contains("jacoco"))
				.limit(1)
				.collect(Collectors.toList());
	}

	private String consumeOutput(Process hosh) throws IOException {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(hosh.getInputStream(), StandardCharsets.UTF_8))) {
			return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private void sendInput(Process hosh, String... lines) throws IOException {
		try (Writer writer = new OutputStreamWriter(hosh.getOutputStream(), StandardCharsets.UTF_8)) {
			for (String line : lines) {
				writer.write(line);
				writer.write(System.lineSeparator());
				writer.flush();
			}
		}
	}

	private void closeInput(Process hosh) throws IOException {
		hosh.getOutputStream().close();
	}

	@Todo(description = "use https://github.com/alirdn/windows-kill")
	private void sendSigint(Process hosh) throws InterruptedException, IOException {
		// wait some time to start the process and then send a SIGINT
		boolean terminated = hosh.waitFor(1, TimeUnit.SECONDS);
		assertThat(terminated).isFalse();
		int waitFor = new ProcessBuilder()
				.command("kill", "-INT", Long.toString(hosh.pid()))
				.start()
				.waitFor();
		assertThat(waitFor).isEqualTo(0);
	}
}