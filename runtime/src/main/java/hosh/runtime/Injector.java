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
package runtime;

import hosh.spi.Command;
import hosh.spi.HistoryAware;
import hosh.spi.LineReaderAware;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.TerminalAware;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

// Inject notable and important stateful objects defined in public API into a command instance.
// NB: Interpreter is not injected here to avoid circular dependencies.
public class Injector {

	private History history;

	// this is a "private" LineReader to be injected in commands: it has no history
	// and no auto-complete
	private LineReader lineReader;

	private State state;

	private Terminal terminal;

	public void injectDeps(Command command) {
		if (command instanceof HistoryAware) {
			((HistoryAware) command).setHistory(history);
		}
		if (command instanceof LineReaderAware) {
			((LineReaderAware) command).setLineReader(lineReader);
		}
		if (command instanceof StateAware) {
			((StateAware) command).setState(state);
		}
		if (command instanceof TerminalAware) {
			((TerminalAware) command).setTerminal(terminal);
		}
	}

	public void setHistory(History history) {
		this.history = history;
	}

	public void setLineReader(LineReader lineReader) {
		this.lineReader = lineReader;
	}

	public void setState(State state) {
		this.state = state;
	}

	public void setTerminal(Terminal terminal) {
		this.terminal = terminal;
	}

}
