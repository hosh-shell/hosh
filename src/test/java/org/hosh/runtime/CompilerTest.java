package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import java.util.Collections;
import java.util.List;

import org.hosh.runtime.Compiler.CompileError;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Command;
import org.hosh.spi.CommandWrapper;
import org.hosh.spi.State;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CompilerTest {
	@Mock
	private State state;
	@Mock(stubOnly = true)
	private Command command;
	@Mock(stubOnly = true)
	private CommandWrapper<?> commandWrapper;
	@Mock
	private CommandResolver commandResolver;
	@InjectMocks
	private Compiler sut;

	@Ignore("missing support for pipeline")
	@Test
	public void commandWithPipeline() {
		given(commandResolver.tryResolve("env")).willReturn(command);
		Program program = sut.compile("env | env");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements).hasSize(2);
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).isEmpty();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).isEmpty();
	}

	@Test
	public void commandWithVariableExpansionWithSpace() {
		given(commandResolver.tryResolve("cd")).willReturn(command);
		given(state.getVariables()).willReturn(Collections.singletonMap("DIR", "/tmp"));
		Program program = sut.compile("cd ${DIR}");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).contains("/tmp");
	}

	@Test
	public void commandWithVariableExpansionNoSpace() {
		given(commandResolver.tryResolve("echo")).willReturn(command);
		given(state.getVariables()).willReturn(Collections.singletonMap("DIR", "/tmp"));
		Program program = sut.compile("echo ${DIR}/aaa");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).contains("/tmp", "/aaa");
	}

	@Test
	public void commandWithUnknownVariableExpansion() {
		given(commandResolver.tryResolve("cd")).willReturn(command);
		assertThatThrownBy(() -> sut.compile("cd ${DIR}"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: unknown variable DIR");
	}

	@Test
	public void commandWithoutArguments() {
		given(commandResolver.tryResolve("env")).willReturn(command);
		Program program = sut.compile("env");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).isEmpty();
	}

	@Test
	public void commandWithArguments() {
		given(commandResolver.tryResolve("env")).willReturn(command);
		Program program = sut.compile("env --system");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).containsExactly("--system");
	}

	@Test
	public void commandNotRegistered() {
		given(commandResolver.tryResolve("env")).willReturn(command);
		assertThatThrownBy(() -> sut.compile("env\nenv\nenv2"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 3: unknown command env2");
	}

	@Test
	public void wrappedCommandNoArgs() {
		willReturn(commandWrapper).given(commandResolver).tryResolve("withTime");
		willReturn(command).given(commandResolver).tryResolve("git");
		Program program = sut.compile("withTime { git push }");
		assertThat(program.getStatements()).hasSize(1);
	}

	@Test
	public void strings() {
		willReturn(command).given(commandResolver).tryResolve("vim");
		Program program = sut.compile("vim 'file with spaces'");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).containsExactly("file with spaces");
	}
}
