package org.hosh.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class HoshIT {

	@Test
	public void version() throws Exception {
		Process java = new ProcessBuilder().command("java", "-jar", "target/dist/hosh.jar").redirectErrorStream(true)
				.start();
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(java.getOutputStream(), StandardCharsets.UTF_8));
		writer.write("cwd\n");
		writer.write("exit\n");
		writer.flush();
		// TODO: output contains dumb terminal warning
		// BufferedReader bufferedReader = new BufferedReader(new
		// InputStreamReader(java.getInputStream()));
		// String output = bufferedReader.lines().collect(Collectors.joining(","));
		int exitCode = java.waitFor();
		assertThat(exitCode).isEqualTo(0);
	}

}
