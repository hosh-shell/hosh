/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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

import hosh.spi.Ansi;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import org.jline.terminal.Terminal;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.stream.Collectors;

public class ConsoleChannel implements OutputChannel {

	private final PrintWriter printWriter;

	private final Ansi.Style style;

	public ConsoleChannel(Terminal terminal, Ansi.Style style) {
		this.printWriter = terminal.writer();
		this.style = style;
	}

	@Override
	public void send(Record record) {
		Locale locale = Locale.getDefault();
		String line = record
				.values().map(value -> value.show(locale))
				.collect(Collectors.joining(" ", style.enable(), style.disable()));
		printWriter.println(line);
		printWriter.flush();
	}

	@Override
	public String toString() {
		return String.format("ConsoleChannel[style=%s]", style);
	}
}
