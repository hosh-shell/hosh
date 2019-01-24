package org.hosh.runtime;

import org.hosh.runtime.Parser.ParseError;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParserTest {
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	private Parser sut = new Parser();

	@Test
	public void empty() {
		sut.parse("");
	}

	@Test
	public void usage() {
		sut.parse("git");
		sut.parse("git\n");
		sut.parse("git status\n");
		sut.parse("git commit --amend\n");
		sut.parse("cd ..\n");
		sut.parse("cd /tmp\n");
		sut.parse("cd c:\\temp\n");
		sut.parse("cd c:/temp\n");
		sut.parse("${EXECUTABLE}");
		sut.parse("cd ${DIR}");
		sut.parse("withTime { git push }");
		sut.parse("withLock /tmp/push.lock { git push }");
		sut.parse("vim 'filename with spaces'");
	}

	@Ignore("recursive wrapped commands still not allowed")
	@Test
	public void recursiveWrappedCommands() {
		sut.parse("withLock /tmp/push.lock { git push\n git push --tags\n }");
	}

	@Test
	public void newlines() {
		sut.parse("\n");
		sut.parse("\n\n");
		sut.parse("\r\n");
		sut.parse("\r\n\n");
		sut.parse("\n\r\n");
	}

	@Test
	public void comments() {
		sut.parse("#\n");
		sut.parse("#\r\n");
		sut.parse("# comment\n");
		sut.parse("# comment\r\n");
		sut.parse("ls # comment\r\n");
	}

	@Test
	public void pipelines() {
		sut.parse("env | grep /regexp/");
		sut.parse("cat file.txt | grep /regexp/");
		sut.parse("cat file.txt | grep /regexp/ | wc -l");
	}

	@Test
	public void lexerError() {
		expectedException.expect(ParseError.class);
		expectedException.expectMessage("line 1:0: token recognition error at: '!'");
		sut.parse("!");
	}
}