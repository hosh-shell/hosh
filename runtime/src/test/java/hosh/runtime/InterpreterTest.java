/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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

import hosh.runtime.Compiler.Program;
import hosh.runtime.Compiler.Statement;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.State;
import hosh.spi.StateMutator;
import hosh.spi.Values;
import hosh.spi.VariableName;
import hosh.spi.test.support.RecordMatcher;
import hosh.test.support.WithThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InterpreterTest {

	@RegisterExtension
	final WithThread withThread = new WithThread();

	@Mock(stubOnly = true)
	State state;

	@Mock
	StateMutator stateMutator;

	@Mock
	Injector injector;

	@Mock
	InputChannel in;

	@Mock
	OutputChannel out;

	@Mock
	OutputChannel err;

	@Mock(stubOnly = true)
	Program program;

	@Mock(stubOnly = true)
	Statement statement;

	@Mock
	Command command;

	Interpreter sut;

	@BeforeEach
	void setup() {
		sut = new Interpreter(state, stateMutator, injector);
	}

	@Test
	void injectDependencies() {
		given(state.getVariables()).willReturn(Map.of());
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of());
		sut.eval(program, out, err);
		then(injector).should().injectDeps(command);
	}

	@Test
	void storeCommandExitStatus() {
		given(state.getVariables()).willReturn(Map.of());
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.of(2));
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of());
		ExitStatus exitStatus = sut.eval(program, out, err);
		assertThat(exitStatus).isError();
		then(stateMutator).should().mutateVariables(Map.of(Interpreter.EXIT_STATUS, "2"));
	}

	@Test
	void handleCancellations() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new CancellationException("simulated cancellation"));
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of());
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program, out, err);
		assertThat(exitStatus).isError();
	}

	@Test
	void handleExceptionWithoutMessage() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new NullPointerException());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of());
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program, out, err);
		assertThat(exitStatus).isError();
		then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("(no message provided)"), Keys.LOCATION, Values.ofText("cmd")));
	}

	@Test
	void handleExceptionWithMessage() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
				.willThrow(new IllegalArgumentException("simulated error"));
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of());
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program, out, err);
		assertThat(exitStatus).isError();
		then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("simulated error"), Keys.LOCATION, Values.ofText("cmd")));
	}

	@Test
	void constantArgument() {
		given(state.getVariables()).willReturn(Map.of());
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of(new Compiler.Constant("file")));
		sut.eval(program, out, err);
		then(command).should().run(Mockito.eq(List.of("file")), Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	void presentVariable() {
		VariableName variable = VariableName.constant("VARIABLE");
		given(state.getVariables()).willReturn(Map.of(variable, "1"));
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of(new Compiler.Variable(variable)));
		ExitStatus exitStatus = sut.eval(program, out, err);
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
		then(command).should().run(Mockito.eq(List.of("1")), Mockito.any(), Mockito.any(), Mockito.any());
	}

	@Test
	void absentVariables() {
		VariableName variableName = VariableName.constant("VARIABLE");
		given(state.getVariables()).willReturn(Collections.singletonMap(variableName, null));
		given(program.getStatements()).willReturn(List.of(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of(new Compiler.Variable(variableName)));
		given(statement.getLocation()).willReturn("cmd");
		ExitStatus exitStatus = sut.eval(program, out, err);
		assertThat(exitStatus).isError();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("cannot resolve variable: VARIABLE")));
	}

	@Test
	void setThreadNameWithArgs() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of(new Compiler.Constant("-jar"), new Compiler.Constant("hosh.jar")));
		given(statement.getLocation()).willReturn("java");
		sut.eval(statement, in, out, err);
		assertThat(withThread.currentName()).isEqualTo("command='java -jar hosh.jar'");
		then(err).shouldHaveNoInteractions(); // checking no assertion failures happened
	}

	@Test
	void setThreadNameWithoutArgs() {
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(List.of());
		given(statement.getLocation()).willReturn("java");
		sut.eval(statement, in, out, err);
		assertThat(withThread.currentName()).isEqualTo("command='java'");
		then(err).shouldHaveNoMoreInteractions(); // checking no assertion failures happened
	}
}
