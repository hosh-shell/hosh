package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CommandFactoryTest {

	@Mock
	private State state;

	@Mock
	private Terminal terminal;

	@InjectMocks
	private CommandFactory sut;

	@Test
	public void simpleCommand() throws Exception {
		sut.setStrategy(Mockito::mock);
		Command result = sut.create(SimpleCommand.class);
		assertThat(result).isNotNull();
	}

	@Test
	public void terminalCommand() throws Exception {
		sut.setStrategy(Mockito::mock);
		Command result = sut.create(TerminalCommand.class);
		assertThat(result).isNotNull();
		then((TerminalAware) result).should().setTerminal(terminal);
	}

	@Test
	public void stateCommand() throws Exception {
		sut.setStrategy(Mockito::mock);
		Command result = sut.create(StateCommand.class);
		assertThat(result).isNotNull();
		then((StateAware) result).should().setState(state);
	}

	@Test
	public void terminalStateCommand() throws Exception {
		sut.setStrategy(Mockito::mock);
		Command result = sut.create(TerminalStateCommand.class);
		assertThat(result).isNotNull();
		then((StateAware) result).should().setState(state);
		then((TerminalAware) result).should().setTerminal(terminal);
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidCommandClass() throws Exception {
		sut.create(CommandWithNotEmptyConstructor.class);
	}

	static class SimpleCommand implements Command {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
		}

	}

	static class TerminalCommand implements Command, TerminalAware {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
		}

		@Override
		public void setTerminal(Terminal terminal) {

		}

	}

	static class StateCommand implements Command, StateAware {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
		}

		@Override
		public void setState(State state) {
		}
	}

	static class TerminalStateCommand implements Command, StateAware, TerminalAware {

		@Override
		public void run(List<String> args, Channel out, Channel err) {
		}

		@Override
		public void setState(State state) {
		}

		@Override
		public void setTerminal(Terminal terminal) {

		}
	}

	@SuppressWarnings("unused")
	static class CommandWithNotEmptyConstructor implements Command {

		private final String name;

		public CommandWithNotEmptyConstructor(String name) {
			this.name = name;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
		}

	}

}
