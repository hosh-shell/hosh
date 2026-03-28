/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
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

import hosh.Hosh;
import hosh.doc.Bug;
import hosh.doc.Todo;
import hosh.test.support.TemporaryFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance tests by running the packaged jar (i.e. java -jar hosh.jar).
 */
@Tag("acceptance")
class HoshIT {

	@RegisterExtension
	final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@AfterEach
	void afterEach(TestInfo testInfo) {
		System.out.println(testInfo.getDisplayName());
	}

	@Test
	void initializeWithoutSystemEnv() throws Exception {
		// Given
		ProcessBuilder hoshBuilder = givenHoshProcessBuilder();
		hoshBuilder.environment().clear();
		// When
		Process sut = hoshBuilder.start();
		closeInput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void interactiveEndOfFile() throws Exception {
		// Given
		Process sut = givenHoshProcess();
		// When
		closeInput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void interactiveScriptThenExit() throws Exception {
		// Given
		Process sut = givenHoshProcess();
		// When
		sendInput(sut, "ls", "exit");
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void interactiveExitSuccess() throws Exception {
		// Given
		Process sut = givenHoshProcess();
		// When
		sendInput(sut, "exit");
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void interactiveExitFailure() throws Exception {
		// Given
		Process sut = givenHoshProcess();
		// When
		sendInput(sut, "exit 42");
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(42);
	}

	@Test
	void scriptWithCdAndCwd() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"cd " + temporaryFolder.toPath().toAbsolutePath(),
				"cwd"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains(temporaryFolder.toPath().toAbsolutePath().toString());
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void scriptWithOsVar() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE}"//
		);
		Process sut = givenHoshProcess(Map.of("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("hello world!");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void scriptWithMissingOsVar() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE}"//
		);
		Process sut = givenHoshProcess(Collections.emptyMap(), scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("cannot resolve variable: OS_ENV_VARIABLE");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void scriptWithMissingOsVarWithFallback() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE!fallback}"//
		);
		Process sut = givenHoshProcess(Collections.emptyMap(), scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("fallback");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void scriptWithImplicitExit() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo hello"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEqualTo("hello");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void scriptWithExplicitExit() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"exit 1"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void scriptParsedThenExecuted() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"exit 0",
				"AAAAB"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("line 2: 'AAAAB' unknown command");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void nonPipelineExternalCommand() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"git --version"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).startsWith("git version");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void pipelineWithInternalCommand() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"rand | take 1 | count"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEqualTo("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void pipelineReadFromExternalCommand() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"git --version | take 1 | count"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEqualTo("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void pipelineWriteToExternalCommand() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo some_random_hash | git cat-file --batch" //
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEqualTo("some_random_hash missing");
		assertThat(exitCode).isEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/212")
	@Test
	void pipelineWithMiddleExternalCommands() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"git tag | wc -l | wc -l" //
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEqualToIgnoringWhitespace("1");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void missingScript() throws Exception {
		// Given
		Path scriptPath = Paths.get("missing.hosh");
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("unable to load: missing.hosh");
		assertThat(exitCode).isEqualTo(1);
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/23")
	@Test
	void pipelinesDoNotExpandVariables() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE} | take 1"//
		);
		Process sut = givenHoshProcess(Map.of("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEqualTo("hello world!");
		assertThat(exitCode).isEqualTo(0);
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/23")
	@Test
	void wrappersDoNotExpandVariables() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"withTime { echo ${OS_ENV_VARIABLE} } "//
		);
		Process sut = givenHoshProcess(Map.of("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("hello world!");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void errorInSimpleCommand() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"err"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("please do not report");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void errorInProducer() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"err | ls"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("please do not report");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void errorInConsumer() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"ls | err"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("please do not report");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void consumerAndProducerBothInError() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"err | err"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("please do not report");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void consumeInfiniteProducer() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"rand | take 100 | count"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEqualTo("100");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void benchmark() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"benchmark 2 { rand | take 100 | count } "//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).matches("100\r?\n100\r?\n2 PT\\d+.\\d+S PT\\d+.\\d+S PT\\d+.\\d+S");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void unknownCommandInScript() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"FOOBAR"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("line 1: 'FOOBAR' unknown command");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void unknownCommandInteractive() throws Exception {
		// Given
		Process sut = givenHoshProcess();
		// When
		sendInput(sut, "FOOBAR\nexit\n");
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("'FOOBAR' unknown command");
		assertThat(exitCode).isEqualTo(0);
	}

	@Bug(description = "regression test", issue = "https://github.com/dfa1/hosh/issues/71")
	@Test
	void commandWrapperCapturesOutput() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"benchmark 1 { cwd } | schema"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("path", "count best worst average");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void redirectOutputToVariable() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo 'world' | capture WHO", // this is WHO=$(echo 'world')
				"echo hello ${WHO}"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(0);
		assertThat(result).contains("hello world");
	}

	@Test
	void comments() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"# echo 'hello'",
				"# ls",
				"exit 42"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEmpty();
		assertThat(exitCode).isEqualTo(42);
	}

	@Test
	void sequence() throws Exception {
		// Given
		Path scriptPath = givenScript(
				"echo a ; echo b"//
		);
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).contains("a", "b");
		assertThat(exitCode).isEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	void interruptBuiltInCommand() throws Exception {
		// Given
		Path scriptPath = givenScript("rand");
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		sendSigint(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isNotEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	void interruptExternalCommand() throws Exception {
		// Given
		Path scriptPath = givenScript("git cat-file --batch");
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		sendSigint(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isNotEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	void interruptPipeline() throws Exception {
		// Given
		Path scriptPath = givenScript("rand | count");
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		sendSigint(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isNotEqualTo(0);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Bug(issue = "https://github.com/dfa1/hosh/issues/53", description = "signal handling")
	@Test
	void interruptCommandWrapperWithPipeline() throws Exception {
		// Given
		Path scriptPath = givenScript("benchmark 10000 { rand | take 10000 | count }");
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		sendSigint(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isNotEqualTo(0);
	}

	@Test
	void lambdaAfterGlobExpansion() throws Exception {
		// Given
		Path path = givenFolder("A.class", "B.class", "C.java");
		Path scriptPath = givenScript("walk " + path.toAbsolutePath() + " | glob '*.class' | { path -> rm ${path} }");
		Process sut = givenHoshProcess(scriptPath.toString());
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).isEmpty();
		assertThat(path.resolve("A.class")).doesNotExist();
		assertThat(path.resolve("B.class")).doesNotExist();
		assertThat(path.resolve("C.java")).exists();
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void versionLongOption() throws Exception {
		// Given
		Process sut = givenHoshProcess("--version");
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).startsWith("hosh v");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void versionShortOption() throws Exception {
		// Given
		Process sut = givenHoshProcess("-v");
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).startsWith("hosh v");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void helpLongOption() throws Exception {
		// Given
		Process sut = givenHoshProcess("--help");
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(exitCode).isEqualTo(0);
		assertThat(result).startsWith("usage: ");
	}

	@Test
	void helpShortOption() throws Exception {
		// Given
		Process sut = givenHoshProcess("-h");
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).startsWith("usage: ");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	void invalidOption() throws Exception {
		// Given
		Process sut = givenHoshProcess("--blahblah");
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).startsWith("hosh: Unrecognized option: --blahblah");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	void tooManyScripts() throws Exception {
		// Given
		Process sut = givenHoshProcess("aaa.hosh", "bbb.hosh");
		// When
		String result = consumeOutput(sut);
		int exitCode = sut.waitFor();
		// Then
		assertThat(result).startsWith("hosh: too many scripts");
		assertThat(exitCode).isEqualTo(1);
	}

	// simple test infrastructure
	private Path givenScript(String... lines) throws IOException {
		Path scriptPath = temporaryFolder.newFile("test.hosh");
		Files.write(scriptPath, List.of(lines));
		return scriptPath;
	}

	@SuppressWarnings("SameParameterValue")
	private Path givenFolder(String... filenames) throws IOException {
		Path folder = temporaryFolder.newFolder("folder");
		for (String filename : filenames) {
			Files.write(folder.resolve(filename), List.of("some content"));
		}
		return folder;
	}

	private Process givenHoshProcess(String... args) throws IOException {
		return givenHoshProcess(Map.of(Hosh.Environment.HOSH_HISTORY, "false"), args);
	}

	private Process givenHoshProcess(Map<String, String> env, String... args) throws IOException {
		ProcessBuilder pb = givenHoshProcessBuilder(args);
		pb.environment().putAll(env);
		return pb.start();
	}

	private ProcessBuilder givenHoshProcessBuilder(String... args) {
		List<String> cmd = new ArrayList<>();
		cmd.add(absoluteJavaBinary());
		cmd.addAll(propagateJacocoAgentInvocation());
		cmd.addAll(List.of("-jar", "target/hosh.jar"));
		cmd.addAll(List.of(args));
		ProcessBuilder pb = new ProcessBuilder()
				.command(cmd)
				.redirectErrorStream(true);
		// restricting environment variables to pass to the hosh process
		pb.environment().clear();
		pb.environment().put("PATH", System.getenv("PATH"));
		String pathext = System.getenv("PATHEXT");
		if (pathext != null) {
			pb.environment().put("PATHEXT", pathext);
		}
		return pb;
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
				.toList();
	}

	private String consumeOutput(Process sut) throws IOException {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(sut.getInputStream(), StandardCharsets.UTF_8))) {
			return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private void sendInput(Process sut, String... lines) throws IOException {
		try (Writer writer = new OutputStreamWriter(sut.getOutputStream(), StandardCharsets.UTF_8)) {
			for (String line : lines) {
				writer.write(line);
				writer.write(System.lineSeparator());
				writer.flush();
			}
		}
	}

	private void closeInput(Process sut) throws IOException {
		sut.getOutputStream().close();
	}

	@Todo(description = "use https://github.com/alirdn/windows-kill")
	private void sendSigint(Process sut) throws InterruptedException, IOException {
		// wait some time to start the process and then send a SIGINT
		boolean terminated = sut.waitFor(1, TimeUnit.SECONDS);
		assertThat(terminated).isFalse();
		int waitFor = new ProcessBuilder()
				.command("kill", "-INT", Long.toString(sut.pid()))
				.start()
				.waitFor();
		assertThat(waitFor).isEqualTo(0);
	}
}
