package org.hosh;

import org.junit.Test;

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

}