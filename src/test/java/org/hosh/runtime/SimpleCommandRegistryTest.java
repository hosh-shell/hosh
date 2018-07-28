package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SimpleCommandRegistryTest {
	@Mock
	private Command command;
	@Mock
	private Command anotherCommand;
	@Spy
	private State state = new State();
	@InjectMocks
	private SimpleCommandRegistry sut;

	@Test
	public void oneCommand() {
		sut.registerCommand("foo", command);
		assertThat(state.getCommands()).containsEntry("foo", command);
	}

	@Test
	public void sameCommandTwice() {
		sut.registerCommand("foo", command);
		sut.registerCommand("bar", command);
		assertThat(state.getCommands()).containsEntry("foo", command).containsEntry("bar", command);
	}

	@Test
	public void twoDifferentCommands() {
		sut.registerCommand("foo", command);
		sut.registerCommand("bar", anotherCommand);
		assertThat(state.getCommands()).containsEntry("foo", command).containsEntry("bar", anotherCommand);
	}

	@Test
	public void twoTimesSameCommand() {
		assertThatThrownBy(() -> {
			sut.registerCommand("foo", command);
			sut.registerCommand("foo", command);
		}).hasMessage("command with same name already registered: foo")
				.isInstanceOf(IllegalArgumentException.class);
	}
}
