package org.hosh.runtime;

import javax.annotation.Nonnull;

import org.antlr.v4.runtime.*;
import org.hosh.antlr4.HoshLexer;
import org.hosh.antlr4.HoshParser;

/** Facade for ANTLR4 */
public class Parser {
	
	public static HoshParser.ProgramContext parse(@Nonnull String input) {
		HoshLexer lexer = new HoshLexer(CharStreams.fromString(input));
		lexer.removeErrorListeners();
		lexer.addErrorListener(new CustomErrorListener());
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		HoshParser parser = new HoshParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(new CustomErrorListener());
		return parser.program();
	}

	public static class ParseError extends RuntimeException {
		
		private static final long serialVersionUID = 1L;

		public ParseError(String message) {
			super(message);
		}
	}

	private static class CustomErrorListener extends BaseErrorListener {

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			throw new ParseError(String.format("line %s:%s: %s", line, charPositionInLine, msg));

		}
	}

}
