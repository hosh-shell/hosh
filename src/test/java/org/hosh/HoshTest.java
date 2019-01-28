package org.hosh;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileWriter;

import org.hosh.doc.Bug;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

@Bug(description = "ExecPty with TERM=xterm gets stucked on Windows in ExecHelper. Using Redirect.PIPE everything is working fine")
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
	public void scriptFileMissing() throws Exception {
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
	public void scriptWithUnknownCommand() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("asd" + "\n");
			script.write("exit 0" + "\n"); // this line will be not executed
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
	public void scriptWithExplicitExit() throws Exception {
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
	public void scriptWithImplicitExit() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.flush();
		}
		expectedSystemExit.expectSystemExitWithStatus(0);
		expectedSystemExit.checkAssertionAfterwards(new Assertion() {
			@Override
			public void checkAssertion() throws Exception {
				assertThat(systemOutRule.getLog()).isEmpty();
			}
		});
		Hosh.main(new String[] { scriptPath.getAbsolutePath() });
	}

	@Test
	public void scriptWithSimpleCommand() throws Exception {
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

	@Test
	public void scriptWithWrapper() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("withTime { ls }");
			script.flush();
		}
		expectedSystemExit.expectSystemExitWithStatus(0);
		Hosh.main(new String[] { scriptPath.getAbsolutePath() });
	}

	@Test
	public void scriptIsParsedThenExecuted() throws Exception {
		File scriptPath = temporaryFolder.newFile("test.hosh");
		try (FileWriter script = new FileWriter(scriptPath)) {
			script.write("echo hello" + "\n");
			script.write("asd" + "\n");
			script.flush();
		}
		expectedSystemExit.expectSystemExitWithStatus(1);
		expectedSystemExit.checkAssertionAfterwards(new Assertion() {
			@Override
			public void checkAssertion() throws Exception {
				assertThat(systemOutRule.getLog()).isEmpty();
				assertThat(systemErrRule.getLog()).contains("line 2: unknown command asd");
			}
		});
		Hosh.main(new String[] { scriptPath.getAbsolutePath() });
	}
}
