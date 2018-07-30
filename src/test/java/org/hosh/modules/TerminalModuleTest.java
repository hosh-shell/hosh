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
			sut.run(Arrays.asList(), out, err);
			then(terminal).should().puts(ArgumentMatchers.any());
			then(terminal).should().flush();
			then(terminal).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), out, err);
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
			sut.run(Arrays.asList(), out, err);
			then(terminal).should().puts(ArgumentMatchers.any());
			then(terminal).should().flush();
			then(terminal).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), out, err);
			then(terminal).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of("error", Values.ofText("no parameters expected")));
			then(err).shouldHaveNoMoreInteractions();
		}
	}
}
