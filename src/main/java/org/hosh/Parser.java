package org.hosh;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.hosh.antlr4.HoshLexer;
import org.hosh.antlr4.HoshParser;

/** Facade for ANTLR4 */
public class Parser {

    public static HoshParser.ProgramContext parse(String input) {
        HoshLexer lexer = new HoshLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HoshParser parser = new HoshParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new ParseError("line " + line + ":" + charPositionInLine + " " + msg);
            }
        });
        return parser.program();
    }

    public static class ParseError extends RuntimeException {

        public ParseError(String message) {
            super(message);
        }
    }
}
