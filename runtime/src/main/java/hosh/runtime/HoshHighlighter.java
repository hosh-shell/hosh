/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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
package hosh.runtime;

import hosh.spi.LoggerFactory;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class HoshHighlighter implements Highlighter {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private final Compiler compiler;

	public HoshHighlighter(Compiler compiler) {
		this.compiler = compiler;
	}

	@Override
	public AttributedString highlight(LineReader reader, String buffer) {
		AttributedStringBuilder sb = new AttributedStringBuilder();
		try {
			compiler.compile(buffer);
			return sb.append(buffer).toAttributedString();
		} catch (RuntimeException e) {
			LOGGER.log(Level.FINER, "caught exception", e);
			return sb.append(buffer, AttributedStyle.BOLD.foreground(AttributedStyle.RED)).toAttributedString();
		}
	}

	@Override
	public void setErrorPattern(Pattern errorPattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setErrorIndex(int errorIndex) {
		throw new UnsupportedOperationException();
	}
}
