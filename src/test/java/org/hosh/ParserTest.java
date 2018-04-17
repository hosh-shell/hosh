package org.hosh;

import org.junit.Test;

public class ParserTest {

    @Test
    public void usage() {
        Parser.parse("git");
        Parser.parse("git status");
        Parser.parse("git commit --amend");
    }

    @Test
    public void empty() {
        Parser.parse("");
    }

}