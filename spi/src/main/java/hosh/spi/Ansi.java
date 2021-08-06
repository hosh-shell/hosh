/*
 * MIT License
 *
 * Copyright (c) 2019-2021 Davide Angelocola
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

import java.io.PrintWriter;

public class Ansi {

	private Ansi() {
	}

	public enum Style {
		// special
		NONE("", ""),
		// general style
		RESET("0", "0"),
		BOLD("1", "21"),
		FAINT("2", "22"),
		ITALIC("3", "23"),
		UNDERLINE("4", "24"),
		BLINK("5", "25"),
		REVERSE("7", "27"),
		// foreground
		FG_BLACK("30", "39"),
		FG_RED("31", "39"),
		FG_GREEN("32", "39"),
		FG_YELLOW("33", "39"),
		FG_BLUE("34", "39"),
		FG_MAGENTA("35", "39"),
		FG_CYAN("36", "39"),
		FG_WHITE("37", "39"),
		// background
		BG_BLACK("40", "49"),
		BG_RED("41", "49"),
		BG_GREEN("42", "49"),
		BG_YELLOW("43", "49"),
		BG_BLUE("44", "49"),
		BG_MAGENTA("45", "49"),
		BG_CYAN("46", "49"),
		BG_WHITE("47", "49");

		/**
		 * The Control Sequence Introducer (CSI) escape sequence.
		 */
		private static final String CSI = "\u001b[";

		private final String startCode;

		private final String endCode;

		Style(String startCode, String endCode) {
			this.startCode = startCode;
			this.endCode = endCode;
		}

		private void output(PrintWriter pw, String code) {
			if (this != NONE) {
				pw.append(CSI);
				pw.append(code);
				pw.append("m");
			}
		}

		public void enable(PrintWriter pw) {
			output(pw, this.startCode);
		}

		public void disable(PrintWriter pw) {
			output(pw, this.endCode);
		}
	}
}
