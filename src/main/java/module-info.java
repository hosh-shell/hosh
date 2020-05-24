
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

import hosh.modules.FileSystemModule;
import hosh.modules.HistoryModule;
import hosh.modules.NetworkModule;
import hosh.modules.SystemModule;
import hosh.modules.TerminalModule;
import hosh.modules.TextModule;
import hosh.spi.Module;

module hosh {
	requires java.logging;
	requires java.net.http;

	requires jdk.jfr;

	requires jline.reader;
	requires jline.terminal;

	requires commons.cli;

	requires org.antlr.antlr4.runtime;

	// workarounds for mockito
	opens hosh;
	opens hosh.runtime;
	opens hosh.spi;
	opens hosh.modules;

	// workaround for running tests in idea
	uses Module;
	provides Module
	with FileSystemModule, HistoryModule, NetworkModule, SystemModule, TerminalModule, TextModule;
}
