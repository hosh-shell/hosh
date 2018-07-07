package org.hosh.runtime;

import org.hosh.doc.BUG;
import org.hosh.runtime.Parser.ParseError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

// this test is for language explorations
// by now no asserts are enforced
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
		sut.parse("cd ${DIR}\n");
	}

	@BUG(description = "this produces is equivalent to 'cd ${DIR}' instead of 'cd${DIR}'")
	@Test
	public void languageBug() {
		sut.parse("cd${DIR}\n");
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