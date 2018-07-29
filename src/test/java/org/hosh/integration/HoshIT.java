package org.hosh.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HoshIT {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void interactive() throws Exception {
		Process java = new ProcessBuilder()
				.command("java", "-jar", "target/dist/hosh.jar")
				.redirectErrorStream(true)
				.start();
		try (Writer writer = new OutputStreamWriter(java.getOutputStream(), StandardCharsets.UTF_8)) {
			writer.write("cwd\n");
			writer.write("exit\n");
			writer.flush();
		}
		int exitCode = java.waitFor();
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void scriptWithCdAndCwd() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("cd " + temporaryFolder.getRoot().getAbsolutePath() + "\n");
			script.write("cwd" + "\n");
			script.flush();
		}
		Process java = new ProcessBuilder()
				.command("java", "-jar", "target/dist/hosh.jar", scriptPath.getAbsolutePath())
				.redirectErrorStream(true)
				.start();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(java.getInputStream()));
		String output = bufferedReader.lines().collect(Collectors.joining());
		int exitCode = java.waitFor();
		assertThat(output).contains(temporaryFolder.getRoot().getAbsolutePath());
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void scriptWithEchoEnv() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("echo ${OS_ENV_VARIABLE}\n");
			script.flush();
		}
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.environment().put("OS_ENV_VARIABLE", "hello world!");
		Process java = processBuilder
				.command("java", "-jar", "target/dist/hosh.jar", scriptPath.getAbsolutePath())
				.redirectErrorStream(true)
				.start();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(java.getInputStream()));
		String output = bufferedReader.lines().collect(Collectors.joining());
		int exitCode = java.waitFor();
		assertThat(output).contains("hello world!");
		assertThat(exitCode).isEqualTo(0);
	}

	@Test
	public void scriptWithUnknownCommand() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("AAAAB" + "\n");
			script.flush();
		}
		Process java = new ProcessBuilder()
				.command("java", "-jar", "target/dist/hosh.jar", scriptPath.getAbsolutePath())
				.redirectErrorStream(true)
				.start();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(java.getInputStream()));
		String output = bufferedReader.lines().collect(Collectors.joining("\n"));
		int exitCode = java.waitFor();
		assertThat(output).contains("line 1: unknown command AAAAB");
		assertThat(exitCode).isEqualTo(1);
	}
}
