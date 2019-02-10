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
package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hosh.doc.Bug;
import org.hosh.doc.Todo;
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

@Todo(description = "improve assertions (e.g. check arguments parsing and proper nesting of objects)")
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
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls | take 2 | "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1:12: incomplete pipeline near '|'");
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/42", description = "rejected by the compiler")
	@Test
	public void extraBraces() {
		assertThatThrownBy(() -> sut.compile("withTime { ls } } "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: unnecessary closing '}'");
	}

	@Test
	public void pipelineOfCommandsWithoutArguments() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("take");
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
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		doReturn(Optional.of(anotherCommand)).when(commandResolver).tryResolve("grep");
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
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("cd");
		Program program = sut.compile("cd ${DIR}");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).contains("${DIR}");
	}

	@Test
	public void commandWithVariableExpansionNoSpace() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("echo");
		Program program = sut.compile("echo ${DIR}/aaa");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).contains("${DIR}", "/aaa");
	}

	@Test
	public void commandWithoutArguments() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");
		Program program = sut.compile("env");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).isEmpty();
	}

	@Test
	public void commandWithArguments() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");
		Program program = sut.compile("env --system");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).containsExactly("--system");
	}

	@Test
	public void commandNotRegisteredInAPipeline() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("env");
		assertThatThrownBy(() -> sut.compile("env | env2"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'env2' unknown command");
	}

	@Test
	public void wrappedCommand() {
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		doReturn(Optional.of(commandWrapper)).when(commandWrapper).downCast(CommandWrapper.class);
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");
		Program program = sut.compile("withTime -t -a { git push }");
		assertThat(program.getStatements())
				.hasSize(1)
				.first().extracting(Statement::getArguments).containsExactly(Arrays.asList("-t", "-a"));
	}

	@Test
	public void nestedWrappedCommands() {
		doReturn(Optional.of(commandWrapper)).when(commandResolver).tryResolve("withTime");
		doReturn(Optional.of(commandWrapper)).when(commandWrapper).downCast(CommandWrapper.class);
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("git");
		Program program = sut.compile("withTime { withTime { git push } }");
		assertThat(program.getStatements()).hasSize(1);
	}

	@Test
	public void strings() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("vim");
		Program program = sut.compile("vim 'file with spaces'");
		assertThat(program.getStatements()).hasSize(1);
		List<Statement> statements = program.getStatements();
		assertThat(statements.get(0).getCommand()).isSameAs(command);
		assertThat(statements.get(0).getArguments()).containsExactly("file with spaces");
	}

	@Test
	public void commandUsedAsCommandWrapper() {
		doReturn(Optional.of(command)).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls { grep pattern } "))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'ls' is not a command wrapper");
	}

	@Test
	public void unknownCommand() {
		doReturn(Optional.empty()).when(commandResolver).tryResolve("ls");
		assertThatThrownBy(() -> sut.compile("ls { grep pattern }"))
				.isInstanceOf(CompileError.class)
				.hasMessage("line 1: 'ls' unknown command wrapper");
	}
}
