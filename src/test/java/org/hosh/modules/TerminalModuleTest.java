/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
package org.hosh.modules;

import static org.mockito.BDDMockito.then;

import java.util.Arrays;

import org.hosh.modules.TerminalModule.Bell;
import org.hosh.modules.TerminalModule.Clear;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({
		TerminalModuleTest.ClearTest.class,
		TerminalModuleTest.BellTest.class,
})
public class TerminalModuleTest {
	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ClearTest {
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@Mock
		private Terminal terminal;
		@InjectMocks
		private Clear sut;

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), null, out, err);
			then(terminal).should().puts(ArgumentMatchers.any());
			then(terminal).should().flush();
			then(terminal).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), null, out, err);
			then(terminal).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of("error", Values.ofText("no parameters expected")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class BellTest {
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@Mock
		private Terminal terminal;
		@InjectMocks
		private Bell sut;

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), null, out, err);
			then(terminal).should().puts(ArgumentMatchers.any());
			then(terminal).should().flush();
			then(terminal).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), null, out, err);
			then(terminal).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of("error", Values.ofText("no parameters expected")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}
}
