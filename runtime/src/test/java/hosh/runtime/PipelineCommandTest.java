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

import hosh.doc.Todo;
import hosh.runtime.Compiler.Statement;
import hosh.runtime.PipelineChannel.ProducerPoisonPill;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineCommandTest {

	@Mock
	InputChannel in;

	@Mock
	OutputChannel out;

	@Mock
	OutputChannel err;

	@Mock(stubOnly = true)
	Statement producer;

	@Mock(stubOnly = true)
	Statement consumer;

	@Mock(stubOnly = true)
	Statement consumerProducer;

	@Mock(stubOnly = true)
	Command command;

	@Mock(stubOnly = true, lenient = true)
	Interpreter interpreter;

	PipelineCommand sut;

	@BeforeEach
	void setup() {
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
		sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
	}

	@Test
	void producerAndConsumerSuccess() {
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(producer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(consumer), any(), any(), any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@Test
	void producerError() {
		willReturn(ExitStatus.error()).given(interpreter).eval(eq(producer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(consumer), any(), any(), any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isError();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@Test
	void consumerError() {
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(producer), any(), any(), any());
		willReturn(ExitStatus.error()).given(interpreter).eval(eq(consumer), any(), any(), any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isError();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@Test
	void producerPoisonPill() {
		willThrow(new ProducerPoisonPill()).given(interpreter).eval(eq(producer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(consumer), any(), any(), any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@Test
	void consumerPoisonPill() {
		willThrow(new ProducerPoisonPill()).given(interpreter).eval(eq(consumer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(producer), any(), any(), any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@Todo(description = "add checks also for failures + simplify structure")
	@Test
	void recursive() {
		PipelineCommand downStream = new PipelineCommand(producer, consumer);
		downStream.setInterpreter(interpreter);
		willReturn(downStream).given(consumerProducer).getCommand();
		PipelineCommand pipeline = new PipelineCommand(producer, consumerProducer);
		pipeline.setInterpreter(interpreter);
		willReturn(ExitStatus.success()).given(interpreter).eval(any(), any(), any(), any());
		ExitStatus exitStatus = pipeline.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
	}

	@Todo(description = "add checks also for failures + simplify structure")
	@Disabled("not working yet")
	@Test
	void recursiveWithExternalCommands() {
		PipelineCommand downStream = new PipelineCommand(consumerProducer, consumer);
		downStream.setInterpreter(interpreter);
		willReturn(downStream).given(consumerProducer).getCommand();
		PipelineCommand pipeline = new PipelineCommand(producer, consumerProducer);
		pipeline.setInterpreter(interpreter);
		ExternalCommand externalCommand1 = mock(ExternalCommand.class);
		ExternalCommand externalCommand2 = mock(ExternalCommand.class);
		ExternalCommand externalCommand3 = mock(ExternalCommand.class);
		given(producer.getCommand()).willReturn(externalCommand1);
		given(consumerProducer.getCommand()).willReturn(externalCommand2);
		given(consumer.getCommand()).willReturn(externalCommand3);
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(producer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(consumer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(consumerProducer), any(), any(), any());
		ExitStatus exitStatus = pipeline.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(externalCommand1).should().pipeline(PipelineCommand.Position.FIRST);
		then(externalCommand2).should().pipeline(PipelineCommand.Position.LAST);
		then(externalCommand3).should().pipeline(PipelineCommand.Position.MIDDLE);
	}

}
