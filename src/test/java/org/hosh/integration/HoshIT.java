package org.hosh.integration;

import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: add tests for interactive usage + non-iteractive usage
@Ignore("getting a dumb terminal inside failsafe plugin")
public class HoshIT {

	@Test
	public void version() throws Exception {
		Process java = new ProcessBuilder().command("java", "-jar", "target/dist/hosh.jar", "--version")
				.redirectErrorStream(true).start();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(java.getInputStream()));
		String output = bufferedReader.lines().collect(Collectors.joining(","));
		int exitCode = java.waitFor();
		assertThat(exitCode).isEqualTo(0);
		assertThat(output).startsWith("hosh v");
	}

	@Test
	public void invalidOption() throws Exception {
		Process java = new ProcessBuilder().command("java", "-jar", "target/dist/hosh.jar", "--abracadabra")
				.redirectErrorStream(true).start();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(java.getInputStream()));
		String output = bufferedReader.lines().collect(Collectors.joining(","));
		int exitCode = java.waitFor();
		assertThat(exitCode).isEqualTo(1);
		assertThat(output).isEqualTo("Unmatched argument [--abracadabra]");
	}

}
