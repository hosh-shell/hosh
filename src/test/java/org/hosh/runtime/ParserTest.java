package org.hosh.runtime;

import org.hosh.runtime.Parser.ParseError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

// TODO: missing asserts
public class ParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	private Parser sut = new Parser();

	@Test
	public void usage() {
		sut.parse("git\n");
		sut.parse("git status\n");
		sut.parse("git commit --amend\n");
		sut.parse("cd ..\n");
		sut.parse("cd /tmp\n");
	}

	@Test
	public void newlines() {
		sut.parse("\n");
		sut.parse("\n\n");
	}

	@Test
	public void empty() {
		sut.parse("");
	}

	@Test
	public void comments() {
		sut.parse("#\n");
		sut.parse("#\r\n");
		sut.parse("# comment\n");
		sut.parse("# comment\r\n");
		sut.parse("# comment\r\n  # comment\n");
	}

	@Test
	public void lexerError() {
		expectedException.expect(ParseError.class);
		expectedException.expectMessage("line 1:0: token recognition error at: '!'");

		sut.parse("!");
	}

	@Test
	public void syntaxError() {
		expectedException.expect(ParseError.class);
		expectedException.expectMessage("line 1:3: mismatched input");

		sut.parse("sdf");
	}

}