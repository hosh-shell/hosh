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

import hosh.spi.Command;
import hosh.spi.HistoryAware;
import hosh.spi.LineReaderAware;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.TerminalAware;
import hosh.spi.Version;
import hosh.spi.VersionAware;
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
class InjectorTest {

	@Mock(stubOnly = true)
	History history;

	@Mock(stubOnly = true)
	LineReader lineReader;

	@Mock(stubOnly = true)
	State state;

	@Mock(stubOnly = true)
	Terminal terminal;

	@Mock(stubOnly = true)
	Version version;

	Injector sut;

	@BeforeEach
	void setup() {
		sut = new Injector();
		sut.setHistory(history);
		sut.setLineReader(lineReader);
		sut.setState(state);
		sut.setTerminal(terminal);
		sut.setVersion(version);
	}

	@Test
	void injectNothing() {
		Command command = Mockito.mock(Command.class);
		sut.injectDeps(command);
		then(command).shouldHaveNoInteractions();
	}

	@Test
	void injectHistory() {
		HistoryAwareCommand command = Mockito.mock(HistoryAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setHistory(history);
	}

	@Test
	void injectLineReaderWare() {
		LineReaderAwareCommand command = Mockito.mock(LineReaderAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setLineReader(lineReader);
	}

	@Test
	void injectState() {
		StateAwareCommand command = Mockito.mock(StateAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setState(state);
	}

	@Test
	void injectTerminal() {
		TerminalAwareCommand command = Mockito.mock(TerminalAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setTerminal(terminal);
	}

	@Test
	void injectVersion() {
		VersionAwareCommand command = Mockito.mock(VersionAwareCommand.class);
		sut.injectDeps(command);
		then(command).should().setVersion(version);
	}


	interface HistoryAwareCommand extends Command, HistoryAware {
	}

	interface LineReaderAwareCommand extends Command, LineReaderAware {
	}

	interface StateAwareCommand extends Command, StateAware {
	}

	interface TerminalAwareCommand extends Command, TerminalAware {
	}

	interface VersionAwareCommand extends Command, VersionAware {

	}
}
