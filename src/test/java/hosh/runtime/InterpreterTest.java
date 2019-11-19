/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package hosh.runtime;

import static hosh.testsupport.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import hosh.spi.InputChannel;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import hosh.runtime.Compiler.Program;
import hosh.runtime.Compiler.Resolvable;
import hosh.runtime.Compiler.Statement;
import hosh.spi.OutputChannel;
import hosh.spi.Command;
import hosh.spi.CommandWrapper;
import hosh.spi.ExitStatus;
import hosh.spi.Keys;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.TerminalAware;
import hosh.spi.Values;
import hosh.testsupport.RecordMatcher;
import hosh.testsupport.WithThread;

@ExtendWith(MockitoExtension.class)
public class InterpreterTest {

	@RegisterExtension
	public final WithThread withThread = new WithThread();

	@Mock(stubOnly = true)
	private State state;

	@Mock(stubOnly = true)
	private Terminal terminal;

	@Mock
	private InputChannel in;

	@Mock
	private OutputChannel out;

	@Mock
	private OutputChannel err;

	@Mock(stubOnly = true)
	private Program program;

	@Mock(stubOnly = true)
	private Statement statement;

	@Mock
	private Command command;

	@Spy
	private StateAwareCommand stateAwareCommand;

	@Spy
	private StateAwareCommandWrapper stateAwareCommandWrapper;

	@Spy
	private TerminalAwareCommand terminalAwareCommand;

	private final Map<String, String> variables = new HashMap<>();

	private final List<Resolvable> args = new ArrayList<>();

	private Interpreter sut;

	@BeforeEach
	public void setup() {
		sut = new Interpreter(state, terminal, out, err);
	}

	@Test
	public void storeCommandExitStatus() {
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.error());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
		assertThat(variables).containsEntry("EXIT_STATUS", "1");
	}

	@Test
	public void injectState() {
		given(state.getVariables()).willReturn(variables);
		given(stateAwareCommand.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(stateAwareCommand);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(stateAwareCommand).should().setState(state);
	}

	@Test
	public void injectTerminal() {
		given(terminalAwareCommand.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(terminalAwareCommand);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(terminalAwareCommand).should().setTerminal(terminal);
	}

	@Test
	public void injectStateInCommandWrapper() {
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(stateAwareCommandWrapper);
		given(statement.getArguments()).willReturn(args);
		given(statement.getLocation()).willReturn("cmd:");
		sut.eval(program);
		then(stateAwareCommandWrapper).should().setState(state);
	}

	@Test
	public void handleCancellations() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new CancellationException("simulated cancellation"));
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
	}

	@Test
	public void handleExceptionWithoutMessage() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new NullPointerException());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
		then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("(no message provided)")));
	}

	@Test
	public void handleExceptionWithMessage() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new IllegalArgumentException("simulated error"));
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isEqualTo(ExitStatus.error());
		then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("simulated error")));
	}

	@Test
	public void constantArgument() {
		args.add(new Compiler.Constant("file"));
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(command).should().run(Mockito.eq(List.of("file")), Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void presentVariable() {
		args.add(new Compiler.Variable("VARIABLE"));
		variables.put("VARIABLE", "1");
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
		then(command).should().run(Mockito.eq(List.of("1")), Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	public void absentVariables() {
		args.add(new Compiler.Variable("VARIABLE"));
		variables.put("VARIABLE", null);
		given(state.getVariables()).willReturn(variables);
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program);
		assertThat(exitStatus).isError();
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("cannot resolve variable: VARIABLE")));
	}

	@Test
	public void setThreadNameWithArgs() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of(new Compiler.Constant("-jar"), new Compiler.Constant("hosh.jar")));
		given(statement.getLocation()).willReturn("java");
		sut.run(statement, in, out, err);
		assertThat(Thread.currentThread().getName()).isEqualTo("command='java -jar hosh.jar'");
		then(err).shouldHaveZeroInteractions(); // checking no assertion failures happened
	}

	@Test
	public void setThreadNameWithoutArgs() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of());
		given(statement.getLocation()).willReturn("java");
		sut.run(statement, in, out, err);
		assertThat(Thread.currentThread().getName()).isEqualTo("command='java'");
		then(err).shouldHaveNoMoreInteractions(); // checking no assertion failures happened
	}

	public interface StateAwareCommand extends Command, StateAware {
	}

	public interface StateAwareCommandWrapper extends CommandWrapper<Object>, StateAware {
	}

	public interface TerminalAwareCommand extends Command, TerminalAware {
	}
}
