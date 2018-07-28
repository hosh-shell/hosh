package org.hosh;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

public class HoshTest {
	@Rule
	public final ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Rule
	public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().mute();
	@Rule
	public final SystemErrRule systemErrRule = new SystemErrRule().enableLog().mute();

	@Test
	public void missingScript() throws Exception {
		expectedSystemExit.expectSystemExitWithStatus(1);
		expectedSystemExit.checkAssertionAfterwards(new Assertion() {
			@Override
			public void checkAssertion() throws Exception {
				assertThat(systemOutRule.getLog()).isEmpty();
				assertThat(systemErrRule.getLog()).contains("unable to load: test.hosh");
			}
		});
		Hosh.main(new String[] { "test.hosh" });
	}

	@Test
	public void scriptWithSyntaxError() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("asd" + "\n");
			script.flush();
		}
		expectedSystemExit.expectSystemExitWithStatus(1);
		expectedSystemExit.checkAssertionAfterwards(new Assertion() {
			@Override
			public void checkAssertion() throws Exception {
				assertThat(systemOutRule.getLog()).isEmpty();
				assertThat(systemErrRule.getLog()).contains("line 1: unknown command asd");
			}
		});
		Hosh.main(new String[] { scriptPath.getAbsolutePath() });
	}

	@Test
	public void scriptWithExit() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("exit 1" + "\n");
			script.flush();
		}
		expectedSystemExit.expectSystemExitWithStatus(1);
		expectedSystemExit.checkAssertionAfterwards(new Assertion() {
			@Override
			public void checkAssertion() throws Exception {
				assertThat(systemOutRule.getLog()).isEmpty();
			}
		});
		Hosh.main(new String[] { scriptPath.getAbsolutePath() });
	}

	@Test
	public void simpleScript() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("cd " + temporaryFolder.getRoot().getAbsolutePath() + "\n");
			script.write("ls" + "\n");
			script.flush();
		}
		expectedSystemExit.expectSystemExitWithStatus(0);
		expectedSystemExit.checkAssertionAfterwards(new Assertion() {
			@Override
			public void checkAssertion() throws Exception {
				assertThat(systemOutRule.getLog()).contains("test.hosh");
			}
		});
		Hosh.main(new String[] { scriptPath.getAbsolutePath() });
	}
}
