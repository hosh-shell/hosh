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

import java.util.Collections;
import java.util.List;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandWrapper;
import org.hosh.spi.ExitStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DefaultCommandWrapperTest {
	@Mock(stubOnly = true)
	private Channel in;
	@Mock(stubOnly = true)
	private Channel out;
	@Mock(stubOnly = true)
	private Channel err;
	@Mock(stubOnly = true)
	private Statement statement;
	@Mock(stubOnly = true)
	private Command command;
	@Mock
	private CommandWrapper<Object> commandWrapper;
	@InjectMocks
	private DefaultCommandWrapper<Object> sut;

	@Test
	public void callsBeforeAndAfterWhenStatementCompletesNormally() {
		Object resource = new Object();
		List<String> args = Collections.emptyList();
		given(statement.getCommand()).willReturn(command);
		given(commandWrapper.before(args, in, out, err)).willReturn(resource);
		given(command.run(args, in, out, err)).willReturn(ExitStatus.success());
		ExitStatus exitStatus = sut.run(args, in, out, err);
		then(commandWrapper).should().before(args, in, out, err);
		then(commandWrapper).should().after(resource, in, out, err);
		assertThat(exitStatus).isEqualTo(ExitStatus.success());
	}

	@Test
	public void callsBeforeAndAfterWhenStatementThrows() {
		Object resource = new Object();
		List<String> args = Collections.emptyList();
		given(statement.getCommand()).willReturn(command);
		given(commandWrapper.before(args, in, out, err)).willReturn(resource);
		given(command.run(args, in, out, err)).willThrow(NullPointerException.class);
		assertThatThrownBy(() -> sut.run(args, in, out, err))
				.isInstanceOf(NullPointerException.class);
		then(commandWrapper).should().before(args, in, out, err);
		then(commandWrapper).should().after(resource, in, out, err);
	}
}
