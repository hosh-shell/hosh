package org.hosh;

import java.io.File;
import java.io.FileWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import static org.assertj.core.api.Assertions.*;

public class HoshTest {

	@Rule
	public final ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public final SystemOutRule systemOutRule = new SystemOutRule();

	@Test
	public void scriptWithExit() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		FileWriter script = new FileWriter(scriptPath);
		script.write("exit 1\n");
		script.flush();
		expectedSystemExit.expectSystemExitWithStatus(1);
		Hosh.main(new String[] { scriptPath.getAbsolutePath() });

	}

	@Test
	public void simpleScript() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		FileWriter script = new FileWriter(scriptPath);
		script.write("ls" + "\n");
		script.flush();

		expectedSystemExit.expectSystemExitWithStatus(0);

		Hosh.main(new String[] { scriptPath.getAbsolutePath() });

		assertThat(systemOutRule.getLog()).containsOnlyOnce("pom.xml");
	}

}
