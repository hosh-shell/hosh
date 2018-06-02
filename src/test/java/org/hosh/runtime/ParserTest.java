package org.hosh.runtime;

import org.hosh.runtime.Parser.ParseError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

// TODO: asserts
public class ParserTest {

    @Test
    public void usage() {
        Parser.parse("git\n");
        Parser.parse("git status\n");
        Parser.parse("git commit --amend\n");
    }

    @Test
    public void empty() {
        Parser.parse("");
    }

    @Test
    public void comments() {
        Parser.parse("#\n");
        Parser.parse("#\r\n");
        Parser.parse("# comment\n");
        Parser.parse("# comment\r\n");
        Parser.parse("# comment\r\n  # comment\n");
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test()
    public void lexerError() {
        expectedException.expect(ParseError.class);
        expectedException.expectMessage("line 1:0: token recognition error at: '!'");

        Parser.parse("!");
    }

    @Test()
    public void syntaxError() {
        expectedException.expect(ParseError.class);
        expectedException.expectMessage("line 1:3: mismatched input");

        Parser.parse("sdf");
    }

}