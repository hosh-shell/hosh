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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;

import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Strict.class)
public class InterpreterTest {
	private Map<String, String> variables = new HashMap<>();
	private List<String> args = new ArrayList<>();
	@Mock(stubOnly = true)
	private State state;
	@Mock(stubOnly = true)
	private Terminal terminal;
	@Mock(stubOnly = true)
	private Channel out;
	@Mock
	private Channel err;
	@Mock(stubOnly = true)
	private Program program;
	@Mock(stubOnly = true)
	private Statement statement;
	@Mock
	private Command command;
	@Mock
	private StateAwareCommand stateAwareCommand;
	@Mock
	private TerminalAwareCommand terminalAwareCommand;
	private Interpreter sut;

	@Before
	public void setup() {
		sut = new Interpreter(state, terminal, out, err);
	}

	@Test
	public void storeCommandExitStatus() throws Exception {
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.error());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
		assertThat(variables).containsEntry("EXIT_STATUS", "1");
	}

	@Test
	public void injectState() throws Exception {
		given(state.getVariables()).willReturn(variables);
		doReturn(Optional.of(stateAwareCommand)).when(stateAwareCommand).downCast(StateAware.class);
		given(stateAwareCommand.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(stateAwareCommand);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(stateAwareCommand).should().setState(state);
	}

	@Test
	public void injectTerminal() throws Exception {
		doReturn(Optional.of(terminalAwareCommand)).when(terminalAwareCommand).downCast(TerminalAware.class);
		given(terminalAwareCommand.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(terminalAwareCommand);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(terminalAwareCommand).should().setTerminal(terminal);
	}

	@Test
	public void handleCancellations() throws Exception {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new CancellationException());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
	}

	@Test
	public void handleExceptionWithoutMessage() throws Exception {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new NullPointerException());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
		then(err).should().send(Record.of("error", Values.ofText("(no message provided)")));
	}

	@Test
	public void handleExceptionWithMessage() throws Exception {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new IllegalArgumentException("simulated error"));
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
		then(err).should().send(Record.of("error", Values.ofText("simulated error")));
	}

	@Test
	public void plainArguments() throws Exception {
		args.add("file");
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(command).should().run(Mockito.eq(args), Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void presentVariable() throws Exception {
		args.add("${VARIABLE}");
		variables.put("VARIABLE", "1");
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(command).should().run(Mockito.eq(Arrays.asList("1")), Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void absentVariables() throws Exception {
		args.add("${VARIABLE}");
		given(state.getVariables()).willReturn(variables);
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getArguments()).willReturn(args);
		assertThatThrownBy(() -> sut.eval(program))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("unknown variable: VARIABLE");
	}

	public interface StateAwareCommand extends Command, StateAware {
	}

	public interface TerminalAwareCommand extends Command, TerminalAware {
	}
}