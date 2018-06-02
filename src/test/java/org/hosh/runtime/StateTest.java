package org.hosh.runtime;

import org.hosh.runtime.State.*;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;


import java.nio.file.Path;
import java.nio.file.Paths;

public class StateTest {

	@Test
	public void registerReader() {
		State state = new State();
		
		Reader<String> version = state.registerReader("VERSION", "1.0");
		
		assertThat(version).isNotNull();
		assertThat(version.get()).isEqualTo("1.0");
		assertThat(version.get()).isEqualTo("1.0"); // idempotent
	}

	@Test
	public void usage() {
		State state = new State();
		
		Reader<Path> cwdReader = state.registerReader("CWD", Paths.get(""));
		Writer<Path> cwdWriter = state.registerWriter("CWD");
		
		assertThat(cwdReader).isNotNull();
		assertThat(cwdWriter).isNotNull();
		cwdWriter.set(Paths.get("/tmp"));
		assertThat(cwdReader.get()).isEqualTo(Paths.get("/tmp"));
	}
}
