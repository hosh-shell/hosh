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

import hosh.doc.Todo;
import hosh.testsupport.TemporaryFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class HoshIT {

	@RegisterExtension
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@AfterEach
	void beforeEach(TestInfo testInfo) {
		System.out.println(testInfo.getDisplayName());
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
	public void invalidOption() throws Exception {
		Process hosh = givenHoshProcess("--blahblah");
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(1);
		assertThat(output).startsWith("hosh: Unrecognized option: --blahblah");
	}

	@Test
	public void tooManyScripts() throws Exception {
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
		ProcessBuilder pb = givenHoshProcessBuilder(args);
		pb.environment().putAll(additionalEnv);
		return pb.start();
	}

	private ProcessBuilder givenHoshProcessBuilder(String... args) {
		List<String> cmd = new ArrayList<>();
		cmd.add(absoluteJavaBinary());
		cmd.addAll(propagateJacocoAgentInvocation());
		cmd.addAll(List.of("-jar", "target/dist/hosh.jar"));
		cmd.addAll(List.of(args));
		return new ProcessBuilder()
			       .command(cmd)
			       .redirectErrorStream(true);
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
