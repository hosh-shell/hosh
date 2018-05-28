package org.hosh;

import org.antlr.v4.runtime.*;
import org.hosh.antlr4.HoshLexer;
import org.hosh.antlr4.HoshParser;

/** Facade for ANTLR4 */
public class Parser {

    public static HoshParser.ProgramContext parse(String input) {
        HoshLexer lexer = new HoshLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HoshParser parser = new HoshParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.program();

    }

    public static class ParseError extends RuntimeException {

        public ParseError(String message) {
            super(message);
        }
    }
}
