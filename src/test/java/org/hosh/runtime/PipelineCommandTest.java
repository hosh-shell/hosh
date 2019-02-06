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
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PipelineCommandTest {
	@Mock
	private Channel in;
	@Mock
	private Channel out;
	@Mock
	private Channel err;
	@Mock(stubOnly = true)
	private Terminal terminal;
	@Mock(stubOnly = true)
	private Command command1;
	@Mock(stubOnly = true)
	private Command command2;
	@Mock(stubOnly = true)
	private ArgumentResolver argumentResolver;

	private PipelineCommand setupSut() {
		Statement producer = new Statement();
		producer.setCommand(command1);
		producer.setArguments(Collections.emptyList());
		Statement consumer = new Statement();
		consumer.setCommand(command2);
		consumer.setArguments(Collections.emptyList());
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setTerminal(terminal);
		sut.setArgumentResolver(argumentResolver);
		return sut;
	}

	@Test
	public void producerAndConsumerSuccess() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		then(err).shouldHaveZeroInteractions();
		assertThat(exitStatus.isSuccess()).isEqualTo(true);
	}

	@Test
	public void producerFailure() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.error());
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		then(err).shouldHaveZeroInteractions();
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void consumerFailure() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.error());
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		then(err).shouldHaveZeroInteractions();
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void producerUnhandledException() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willAnswer(inv -> {
			throw new NullPointerException("simulated exception");
		});
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		then(err).should().send(Record.of("error", Values.ofText("simulated exception")));
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void consumerUnhandledException() {
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willAnswer(inv -> {
			throw new NullPointerException("simulated exception");
		});
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		then(err).should().send(Record.of("error", Values.ofText("simulated exception")));
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void consumerUnhandledExecutionException() {
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willAnswer(inv -> {
			throw new ExecutionException("simulated exception", null);
		});
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		then(err).should().send(Record.of("error", Values.ofText("simulated exception")));
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}
}
