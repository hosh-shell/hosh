package org.hosh;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class HoshIT {

    @Test
    public void version() throws Exception {
        Process java = new ProcessBuilder().command("java", "-jar", "target/dist/hosh.jar", "--version").redirectErrorStream(true).start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(java.getInputStream()));
        String output = bufferedReader.lines().collect(Collectors.joining(","));
        boolean finished = java.waitFor(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(output).startsWith("hosh v");
    }

}
