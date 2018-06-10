package org.hosh.runtime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.List;

import org.hosh.runtime.Compiler.CompileError;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CompilerTest {

	@Mock
	private State state;

	@Mock
	private CommandFactory commandFactory;

	@Spy
	private Command command;

	@InjectMocks
	private Compiler sut;

	@Test
	public void commandWithoutArguments() {
		given(state.getCommands()).willReturn(Collections.singletonMap("env", Command.class));
		given(commandFactory.create(Command.class)).willReturn(command);

		Program program = sut.compile("env");

		assertThat(program.getStatements()).hasSize(1);
		// TODO: report bug to assertJ
		// assertThat(program.getStatements()).first().extracting(Statement::getCommand).isSameAs(command);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).isEmpty();
	}

	@Test
	public void commandWithArguments() {
		given(state.getCommands()).willReturn(Collections.singletonMap("env", Command.class));
		given(commandFactory.create(Command.class)).willReturn(command);

		Program program = sut.compile("env --system");

		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).containsExactly("--system");
	}

	@Test
	public void commandNotRegistered() {
		given(state.getCommands()).willReturn(Collections.singletonMap("env", Command.class));

		assertThatThrownBy(() -> sut.compile("env2")).isInstanceOf(CompileError.class).hasMessage("command not found: env2");
	}

}
