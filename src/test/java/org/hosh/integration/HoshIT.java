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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HoshIT {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

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
	public void scriptWithEchoEnv() throws Exception {
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
		int exitCode = hosh.waitFor();
		assertThat(exitCode).isEqualTo(0);
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
				"cwd | count"//
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
				"java --version | count"//
		);
		Process hosh = givenHoshProcess(scriptPath.toString());
		int exitCode = hosh.waitFor();
		String output = consumeOutput(hosh);
		assertThat(output).isEqualTo("3");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void missingScript() throws Exception {
		Path scriptPath = Paths.get("missing.hosh");
		Process hosh = givenHoshProcess(scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).isEqualTo("unable to load: missing.hosh");
		assertThat(exitCode).isEqualTo(1);
	}

	@Test
	public void pipelineExpandVariables() throws Exception {
		Path scriptPath = givenScript(
				"echo ${OS_ENV_VARIABLE} | count"//
		);
		Process hosh = givenHoshProcess(Collections.singletonMap("OS_ENV_VARIABLE", "hello world!"), scriptPath.toString());
		String output = consumeOutput(hosh);
		int exitCode = hosh.waitFor();
		assertThat(output).contains("1");
		assertThat(exitCode).isEqualTo(0);
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
			return bufferedReader.lines().collect(Collectors.joining());
		}
	}

	private void sendInput(Process hosh, String line) throws IOException {
		try (Writer writer = new OutputStreamWriter(hosh.getOutputStream(), StandardCharsets.UTF_8)) {
			writer.write(line + "\n");
			writer.flush();
		}
	}
}
