/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class SequenceCommandTest {

	@Mock(stubOnly = true)
	InputChannel in;

	@Mock(stubOnly = true)
	OutputChannel out;

	@Mock(stubOnly = true)
	OutputChannel err;

	@Mock(stubOnly = true)
	Statement first;

	@Mock(stubOnly = true)
	Statement second;

	@Mock
	Interpreter interpreter;

	SequenceCommand sut;

	@BeforeEach
	void setup() {
		sut = new SequenceCommand(first, second);
		sut.setInterpreter(interpreter);
	}

	@Test
	void happyPath() {
		doReturn(ExitStatus.success()).when(interpreter).eval(first, in, out, err);
		doReturn(ExitStatus.success()).when(interpreter).eval(second, in, out, err);
		ExitStatus result = sut.run(List.of(), in, out, err);
		assertThat(result).isSuccess();
	}

	@Test
	void haltOnFirstError() {
		doReturn(ExitStatus.of(42)).when(interpreter).eval(first, in, out, err);
		ExitStatus result = sut.run(List.of(), in, out, err);
		then(interpreter).should(Mockito.never()).eval(second, in, out, err);
		assertThat(result).hasExitCode(42);
	}
}
