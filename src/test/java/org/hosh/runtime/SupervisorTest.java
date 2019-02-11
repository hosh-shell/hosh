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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.List;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SupervisorTest {
	@Mock(stubOnly = true)
	private Terminal terminal;
	@Mock
	private Channel err;
	@Mock(stubOnly = true)
	private Statement statement;
	@InjectMocks
	private Supervisor sut;

	@After
	public void cleanup() {
		sut.close();
	}

	@Test
	public void noSubmit() {
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(0);
	}

	@Test
	public void allSubmitInSuccess() {
		sut.submit(() -> ExitStatus.success());
		sut.submit(() -> ExitStatus.success());
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(0);
	}

	@Test
	public void oneSubmitInError() {
		sut.submit(() -> ExitStatus.success());
		sut.submit(() -> ExitStatus.of(10));
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(10);
	}

	@Test
	public void oneSubmitWithException() {
		sut.submit(() -> {
			throw new NullPointerException("simulated error");
		});
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(1);
		then(err).should().send(Record.of("error", Values.ofText("simulated error")));
	}

	@Test
	public void oneSubmitWithExceptionButNoMessage() {
		sut.submit(() -> {
			throw new NullPointerException();
		});
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus.value()).isEqualTo(1);
		then(err).should().send(Record.of("error", Values.ofText("(no message provided)")));
	}

	@Test
	public void setThreadNameWithArgs() {
		given(statement.getCommand()).willReturn(new TestCommand());
		given(statement.getArguments()).willReturn(Arrays.asList("-a", "-b"));
		String oldThreadName = Thread.currentThread().getName();
		try {
			sut.setThreadName(statement);
			assertThat(Thread.currentThread().getName()).isEqualTo("command='TestCommand -a -b'");
		} finally {
			Thread.currentThread().setName(oldThreadName);
		}
	}

	@Test
	public void setThreadNameWithoutArgs() {
		given(statement.getCommand()).willReturn(new TestCommand());
		given(statement.getArguments()).willReturn(Arrays.asList());
		String oldThreadName = Thread.currentThread().getName();
		try {
			sut.setThreadName(statement);
			assertThat(Thread.currentThread().getName()).isEqualTo("command='TestCommand'");
		} finally {
			Thread.currentThread().setName(oldThreadName);
		}
	}

	// cannot be a mock since we need a deterministic name
	private static class TestCommand implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			return ExitStatus.success();
		}
	}
}