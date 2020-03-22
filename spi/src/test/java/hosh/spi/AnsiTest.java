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
package hosh.spi;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

public class AnsiTest {

	@Test
	public void none() {
		StringWriter out = new StringWriter();
		PrintWriter pw = new PrintWriter(out);
		Ansi.Style.NONE.enable(pw);
		Ansi.Style.NONE.disable(pw);
		assertThat(out).hasToString("");
	}

	@Test
	public void redForeground() {
		StringWriter out = new StringWriter();
		PrintWriter pw = new PrintWriter(out);
		Ansi.Style.FG_RED.enable(pw);
		Ansi.Style.FG_RED.disable(pw);
		assertThat(out).hasToString("\u001b[31m\u001b[39m");
	}

	@Test
	public void drop() {
		assertThat(Ansi.drop("")).isEqualTo("");
		assertThat(Ansi.drop("aaa")).isEqualTo("aaa");
		assertThat(Ansi.drop("\u001b[31m\u001b[39m")).isEmpty();
		assertThat(Ansi.drop("\u001b[31maaa\u001b[39m")).isEqualTo("aaa");

	}
}
