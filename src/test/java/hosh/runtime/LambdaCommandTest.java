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

import static hosh.testsupport.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class LambdaCommandTest {

	@Mock
	private Interpreter interpreter;

	@Mock(stubOnly = true)
	private Compiler.Statement statement;

	@Mock(stubOnly = true)
	private InputChannel in;

	@Mock(stubOnly = true)
	private OutputChannel out;

	@Mock(stubOnly = true)
	private OutputChannel err;

	@Mock
	private State state;

	private LambdaCommand sut;

	@BeforeEach
	public void setUp() {
		sut = new LambdaCommand(statement, Keys.PATH.name());
		sut.setState(state);
		sut.setInterpreter(interpreter);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void success() {
		Map<String, String> variables = new HashMap<>();
		given(state.getVariables()).willReturn(variables);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.success());
		given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.PATH, Values.ofPath(Path.of("file")))), Optional.empty());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		assertThat(variables).isEmpty();
		then(state).should().setVariables(Collections.singletonMap("path", "file"));
		then(state).should().setVariables(variables);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void error() {
		Map<String, String> variables = new HashMap<>();
		given(state.getVariables()).willReturn(variables);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.error());
		given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.PATH, Values.ofPath(Path.of("file")))), Optional.empty());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isError();
		assertThat(variables).isEmpty();
		then(state).should().setVariables(variables);
	}

}
