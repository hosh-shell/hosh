/*
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
package hosh.runtime;

import hosh.spi.Command;
import hosh.spi.HistoryAware;
import hosh.spi.LineReaderAware;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.TerminalAware;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class InjectorTest {

	@Mock(stubOnly = true)
	private History history;

	@Mock(stubOnly = true)
	private LineReader lineReader;

	@Mock(stubOnly = true)
	private State state;

	@Mock(stubOnly = true)
	private Terminal terminal;

	private Injector sut;

	@BeforeEach
	public void setup() {
		sut = new Injector();
		sut.setHistory(history);
		sut.setLineReader(lineReader);
		sut.setState(state);
		sut.setTerminal(terminal);
	}

	@Test
	public void injectNothing() {
		Command command = Mockito.mock(Command.class);
		sut.injectDeps(command);
		then(command).shouldHaveNoInteractions();
	}

	@Test
	public void injectHistory() {
		HistoryAwareCommand command = Mockito.mock(HistoryAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setHistory(history);
	}

	@Test
	public void injectLineReaderWare() {
		LineReaderAwareCommand command = Mockito.mock(LineReaderAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setLineReader(lineReader);
	}

	@Test
	public void injectState() {
		StateAwareCommand command = Mockito.mock(StateAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setState(state);
	}

	@Test
	public void injectTerminal() {
		TerminalAwareCommand command = Mockito.mock(TerminalAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setTerminal(terminal);
	}

	public interface HistoryAwareCommand extends Command, HistoryAware {
	}

	public interface LineReaderAwareCommand extends Command, LineReaderAware {
	}

	public interface StateAwareCommand extends Command, StateAware {
	}

	public interface TerminalAwareCommand extends Command, TerminalAware {
	}
}
