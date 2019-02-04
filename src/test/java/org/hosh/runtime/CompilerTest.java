package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.hosh.doc.Bug;
import org.hosh.runtime.Compiler.CompileError;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Command;
import org.hosh.spi.CommandWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CompilerTest {
	@Mock(stubOnly = true)
	private Command command;
	@Mock(stubOnly = true)
	private Command anotherCommand;
	@Mock(stubOnly = true)
	private CommandWrapper<?> commandWrapper;
	@Mock(stubOnly = true)
	private CommandResolver commandResolver;
	@InjectMocks
	private Compiler sut;

	@Bug(issue = "https://github.com/dfa1/hosh/issues/26", description = "rejected by the compiler")
	@Test
	public void incompletePipeline() {
		doReturn(command).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls | take 2 | "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1:12: incomplete pipeline near '|'");
	}

	@Test
	public void pipelineOfCommandsWithoutArguments() {
		doReturn(command).when(commandResolver).tryResolve("ls");
		doReturn(anotherCommand).when(commandResolver).tryResolve("take");
		Program program = sut.compile("ls | take 2 | take 1");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements).hasSize(1);
		Statement statement = statements.get(0);
		assertThat(statement.getArguments()).isEmpty();
		assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class);
		statement.getCommand().downCast(PipelineCommand.class).ifPresent(outerCmd -> {
			outerCmd.getConsumer().getCommand().downCast(PipelineCommand.class).ifPresent(innerCmd -> {
				assertThat(innerCmd.getProducer().getArguments()).containsExactly("2");
				assertThat(innerCmd.getConsumer().getArguments()).containsExactly("1");
			});
		});
	}

	@Test
	public void pipelineOfCommandsWithArguments() {
		doReturn(command).when(commandResolver).tryResolve("ls");
		doReturn(anotherCommand).when(commandResolver).tryResolve("grep");
		Program program = sut.compile("ls /home | grep /regex/");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements).hasSize(1);
		Statement statement = statements.get(0);
		assertThat(statement.getCommand()).isInstanceOf(PipelineCommand.class);
		assertThat(statement.getArguments()).isEmpty();
	}

	@Test
	public void commandWithVariableExpansionWithSpace() {
		given(commandResolver.tryResolve("cd")).willReturn(command);
		Program program = sut.compile("cd ${DIR}");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).contains("${DIR}");
	}

	@Test
	public void commandWithVariableExpansionNoSpace() {
		given(commandResolver.tryResolve("echo")).willReturn(command);
		Program program = sut.compile("echo ${DIR}/aaa");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).contains("${DIR}", "/aaa");
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
		assertThatThrownBy(() -> sut.compile("env | env2"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'env2' unknown command");
	}

	@Test
	public void wrappedCommandNoArgs() {
		willReturn(commandWrapper).given(commandResolver).tryResolve("withTime");
		willReturn(command).given(commandResolver).tryResolve("git");
		Program program = sut.compile("withTime { git push }");
		assertThat(program.getStatements()).hasSize(1);
	}

	@Test
	public void nestedWrappedCommands() {
		willReturn(commandWrapper).given(commandResolver).tryResolve("withTime");
		willReturn(command).given(commandResolver).tryResolve("git");
		Program program = sut.compile("withTime { withTime { git push } }");
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

	@Test
	public void commandUsedAsCommandWrapper() {
		doReturn(command).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls { grep pattern } "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'ls' is not a command wrapper");
	}

	@Test
	public void usingCommandAsWrapper() {
		doReturn(null).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls { grep pattern }"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'ls' unknown command wrapper");
	}
}
