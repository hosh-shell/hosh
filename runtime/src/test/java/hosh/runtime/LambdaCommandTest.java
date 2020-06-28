/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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

import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static hosh.spi.test.support.ExitStatusAssert.assertThat;

@ExtendWith(MockitoExtension.class)
class LambdaCommandTest {

	@Mock
	Interpreter interpreter;

	@Mock(stubOnly = true)
	Compiler.Statement statement;

	@Mock
	InputChannel in;

	@Mock
	OutputChannel out;

	@Mock
	OutputChannel err;

	@Mock
	State state;

	LambdaCommand sut;

	@BeforeEach
	void setUp() {
		sut = new LambdaCommand(statement, Keys.PATH.name());
		sut.setState(state);
		sut.setInterpreter(interpreter);
	}

	@SuppressWarnings("unchecked")
	@Test
	void presentKeyWithInnerCommandSuccess() {
		Map<String, String> variables = new HashMap<>();
		given(state.getVariables()).willReturn(variables);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.success());
		given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.PATH, Values.ofPath(Path.of("file")))), Optional.empty());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		assertThat(variables).isEmpty();
		then(state).should().setVariables(Collections.singletonMap("path", "file"));
		then(state).should().setVariables(variables);
		then(in).shouldHaveNoMoreInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@SuppressWarnings("unchecked")
	@Test
	void presentKeyWithInnerCommandError() {
		Map<String, String> variables = new HashMap<>();
		given(state.getVariables()).willReturn(variables);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.error());
		given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.PATH, Values.ofPath(Path.of("file")))), Optional.empty());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isError();
		assertThat(variables).isEmpty();
		then(state).should().setVariables(Collections.singletonMap("path", "file"));
		then(state).should().setVariables(variables);
		then(in).shouldHaveNoMoreInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@SuppressWarnings("unchecked")
	@Test
	void missingKey() {
		Map<String, String> variables = new HashMap<>();
		given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.TEXT, Values.ofPath(Path.of("file")))), Optional.empty());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isError();
		assertThat(variables).isEmpty();
		then(state).shouldHaveNoInteractions();
		then(in).shouldHaveNoMoreInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("missing key 'path'")));
	}

}
