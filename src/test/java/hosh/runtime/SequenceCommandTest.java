/**
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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import hosh.runtime.Interpreter;
import hosh.runtime.SequenceCommand;
import hosh.runtime.Compiler.Statement;
import hosh.spi.Channel;
import hosh.spi.ExitStatus;

@ExtendWith(MockitoExtension.class)
public class SequenceCommandTest {

	@Mock(stubOnly = true)
	private Channel in;

	@Mock(stubOnly = true)
	private Channel out;

	@Mock(stubOnly = true)
	private Channel err;

	@Mock(stubOnly = true)
	private Statement first;

	@Mock(stubOnly = true)
	private Statement second;

	@Mock
	private Interpreter interpreter;

	private SequenceCommand sut;

	@BeforeEach
	public void setup() {
		sut = new SequenceCommand(first, second);
		sut.setInterpreter(interpreter);
	}

	@Test
	public void happyPath() {
		doReturn(ExitStatus.success()).when(interpreter).run(first, in, out, err);
		doReturn(ExitStatus.success()).when(interpreter).run(second, in, out, err);
		ExitStatus result = sut.run(List.of(), in, out, err);
		assertThat(result).isSuccess();
	}

	@Test
	public void haltOnFirstError() {
		doReturn(ExitStatus.of(42)).when(interpreter).run(first, in, out, err);
		ExitStatus result = sut.run(List.of(), in, out, err);
		then(interpreter).should(Mockito.never()).run(second, in, out, err);
		assertThat(result).isError().hasExitCode(42);
	}
}
