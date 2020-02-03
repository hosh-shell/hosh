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

import hosh.runtime.Compiler.Statement;
import hosh.spi.CommandWrapper;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static hosh.testsupport.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class DefaultCommandWrapperTest {

	@Mock(stubOnly = true)
	private InputChannel in;

	@Mock(stubOnly = true)
	private OutputChannel out;

	@Mock(stubOnly = true)
	private OutputChannel err;

	@Mock(stubOnly = true)
	private Statement statement;

	@Mock(stubOnly = true)
	private Interpreter interpreter;

	@Mock
	private CommandWrapper<Object> commandWrapper;

	@InjectMocks
	private DefaultCommandWrapper<Object> sut;

	@BeforeEach
	public void setup() {
		sut = new DefaultCommandWrapper<>(statement, commandWrapper);
		sut.setInterpreter(interpreter);
	}

	@Test
	public void callsBeforeAndAfterWhenStatementCompletesNormally() {
		Object resource = new Object();
		List<String> args = List.of();
		given(commandWrapper.before(args, in, out, err)).willReturn(resource);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.success());
		ExitStatus exitStatus = sut.run(args, in, out, err);
		then(commandWrapper).should().before(args, in, out, err);
		then(commandWrapper).should().after(resource, in, out, err);
		assertThat(exitStatus).isEqualTo(ExitStatus.success());
	}

	@Test
	public void callsBeforeAndAfterWhenStatementThrows() {
		Object resource = new Object();
		List<String> args = List.of();
		given(commandWrapper.before(args, in, out, err)).willReturn(resource);
		given(interpreter.eval(statement, in, out, err)).willThrow(NullPointerException.class);
		assertThatThrownBy(() -> sut.run(args, in, out, err))
			.isInstanceOf(NullPointerException.class);
		then(commandWrapper).should().before(args, in, out, err);
		then(commandWrapper).should().after(resource, in, out, err);
	}

	@Test
	public void keepRetryingAndReturnsLastExitStatus() {
		Object resource = new Object();
		List<String> args = List.of();
		given(commandWrapper.before(args, in, out, err)).willReturn(resource);
		given(commandWrapper.retry(resource, in, out, err)).willReturn(true, false);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.success(), ExitStatus.error());
		ExitStatus exitStatus = sut.run(args, in, out, err);
		assertThat(exitStatus).isError();
		then(commandWrapper).should(Mockito.times(2)).retry(resource, in, out, err);
	}

	@Test
	public void asString() {
		assertThat(sut)
			.hasToString("DefaultCommandWrapper[nested=statement,commandWrapper=commandWrapper]");
	}
}
