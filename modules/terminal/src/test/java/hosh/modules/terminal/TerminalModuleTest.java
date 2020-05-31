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
package hosh.modules.terminal;

import hosh.modules.terminal.TerminalModule.Bell;
import hosh.modules.terminal.TerminalModule.Clear;
import hosh.modules.terminal.TerminalModule.Dump;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.Values;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static hosh.test.support.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

public class TerminalModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ClearTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Mock
		private Terminal terminal;

		@InjectMocks
		private Clear sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(terminal).should().puts(ArgumentMatchers.any());
			then(terminal).should().flush();
			then(terminal).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(terminal).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("no arguments expected")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class BellTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Mock
		private Terminal terminal;

		@InjectMocks
		private Bell sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(terminal).should().puts(ArgumentMatchers.any());
			then(terminal).should().flush();
			then(terminal).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(terminal).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("no arguments expected")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class DumpTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Mock(stubOnly = true)
		private Terminal terminal;

		@InjectMocks
		private Dump sut;

		@Test
		public void noArgs() {
			given(terminal.getType()).willReturn("xterm");
			given(terminal.getAttributes()).willReturn(new Attributes());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Mockito.any());
			then(err).shouldHaveNoMoreInteractions();
		}
	}
}
