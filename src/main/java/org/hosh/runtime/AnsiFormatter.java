/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.hosh.spi.Ansi;

public class AnsiFormatter extends Formatter {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");

	@Override
	public String format(LogRecord logRecord) {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			ZonedDateTime zdt = ZonedDateTime.ofInstant(logRecord.getInstant().truncatedTo(ChronoUnit.MILLIS), ZoneId.systemDefault());
			Ansi.Style style = colorize(logRecord.getLevel());
			pw.append(FORMATTER.format(zdt));
			pw.append(' ');
			pw.append('[');
			style.enable(pw);
			pw.append(logRecord.getLevel().toString());
			style.disable(pw);
			pw.append(']');
			if (logThreadName(logRecord)) {
				pw.append(' ');
				pw.append('[');
				pw.append(Thread.currentThread().getName());
				pw.append(']');
			}
			pw.append(' ');
			pw.append('-');
			pw.append(' ');
			style.enable(pw);
			pw.append(logRecord.getMessage());
			style.disable(pw);
			pw.println();
			if (logRecord.getThrown() != null) {
				logRecord.getThrown().printStackTrace(pw);
			}
			return sw.toString();
		}
	}

	private boolean logThreadName(LogRecord logRecord) {
		// HttpClient message already contains [threadName]
		String loggerName = logRecord.getLoggerName();
		return loggerName == null || !loggerName.startsWith("jdk.internal.httpclient");
	}

	private Ansi.Style colorize(Level level) {
		if (Level.SEVERE.equals(level)) {
			return Ansi.Style.FG_RED;
		}
		if (Level.WARNING.equals(level)) {
			return Ansi.Style.FG_YELLOW;
		}
		return Ansi.Style.NONE;
	}
}
