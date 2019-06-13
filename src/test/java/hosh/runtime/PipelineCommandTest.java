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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import hosh.runtime.Interpreter;
import hosh.runtime.PipelineCommand;
import hosh.runtime.Compiler.Statement;
import hosh.runtime.PipelineChannel.ProducerPoisonPill;
import hosh.spi.Channel;
import hosh.spi.Command;
import hosh.spi.ExitStatus;

@ExtendWith(MockitoExtension.class)
public class PipelineCommandTest {

	@Mock
	private Channel in;

	@Mock
	private Channel out;

	@Mock
	private Channel err;

	@Mock(stubOnly = true)
	private Statement producer;

	@Mock(stubOnly = true)
	private Statement consumer;

	@Mock(stubOnly = true)
	private Statement consumerProducer;

	@Mock(stubOnly = true)
	private Command command;

	@Mock(stubOnly = true, lenient = true)
	private Interpreter interpreter;

	private PipelineCommand sut;

	@BeforeEach
	public void setup() {
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
		sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
	}

	@Test
	public void producerAndConsumerSuccess() {
		willReturn(ExitStatus.success()).given(interpreter).run(Mockito.eq(producer), Mockito.any(), Mockito.any(), Mockito.any());
		willReturn(ExitStatus.success()).given(interpreter).run(Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(true);
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void producerError() {
		willReturn(ExitStatus.error()).given(interpreter).run(Mockito.eq(producer), Mockito.any(), Mockito.any(), Mockito.any());
		willReturn(ExitStatus.success()).given(interpreter).run(Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		then(err).shouldHaveZeroInteractions();
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void consumerError() {
		willReturn(ExitStatus.success()).given(interpreter).run(Mockito.eq(producer), Mockito.any(), Mockito.any(), Mockito.any());
		willReturn(ExitStatus.error()).given(interpreter).run(Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void producerPoisonPill() {
		willThrow(new ProducerPoisonPill()).given(interpreter).run(Mockito.eq(producer), Mockito.any(), Mockito.any(),
				Mockito.any());
		willReturn(ExitStatus.success()).given(interpreter).run(Mockito.eq(consumer), Mockito.any(), Mockito.any(), Mockito.any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(true);
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void consumerPoisonPill() {
		willThrow(new ProducerPoisonPill()).given(interpreter).run(Mockito.eq(consumer), Mockito.any(), Mockito.any(),
				Mockito.any());
		willReturn(ExitStatus.success()).given(interpreter).run(Mockito.eq(producer), Mockito.any(), Mockito.any(), Mockito.any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(true);
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void recursive() {
		PipelineCommand downStream = new PipelineCommand(producer, consumer);
		downStream.setInterpreter(interpreter);
		willReturn(downStream).given(consumerProducer).getCommand();
		PipelineCommand pipeline = new PipelineCommand(producer, consumerProducer);
		pipeline.setInterpreter(interpreter);
		willReturn(ExitStatus.success()).given(interpreter).run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		ExitStatus exitStatus = pipeline.run(List.of(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(true);
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}
}
