/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.hosh.runtime;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.hosh.antlr4.HoshLexer;
import org.hosh.antlr4.HoshParser;

/** Facade for ANTLR4 runtime. */
public class Parser {
	public HoshParser.ProgramContext parse(String input) {
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
