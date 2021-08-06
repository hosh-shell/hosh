/*
 * MIT License
 *
 * Copyright (c) 2019-2021 Davide Angelocola
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
import hosh.runtime.PipelineChannel.ProducerPoisonPill;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;
import org.assertj.core.api.Assertions;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;

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

	@Test
	void producerAndConsumerSuccess() {
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
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
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
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
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
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
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
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
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
		willThrow(new ProducerPoisonPill()).given(interpreter).eval(eq(consumer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(producer), any(), any(), any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
		then(err).shouldHaveNoInteractions();
	}

	@Test
	void runtimeExceptions() {
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
		NullPointerException runtimeException = new NullPointerException("simulated error");
		willThrow(runtimeException).given(interpreter).eval(eq(consumer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(producer), any(), any(), any());
		var result = Assertions.assertThatThrownBy(() -> sut.run(List.of(), in, out, err));
		result
			.hasNoSuppressedExceptions()
			.isSameAs(runtimeException);
	}

	@Test
	void checkedExceptions() {
		PipelineCommand sut = new PipelineCommand(producer, consumer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
		Error error = new OutOfMemoryError("simulated out of memory");
		willThrow(error).given(interpreter).eval(eq(consumer), any(), any(), any());
		willReturn(ExitStatus.success()).given(interpreter).eval(eq(producer), any(), any(), any());
		var result = Assertions.assertThatThrownBy(() -> sut.run(List.of(), in, out, err));
		result
			.hasNoSuppressedExceptions()
			.isNotSameAs(error)
			.isInstanceOf(RuntimeException.class);
	}


	@Test
	void recursive() {
		PipelineCommand sut = new PipelineCommand(producer, consumerProducer);
		sut.setInterpreter(interpreter);
		given(producer.getCommand()).willReturn(command);
		given(consumer.getCommand()).willReturn(command);
		PipelineCommand downStream = new PipelineCommand(producer, consumer);
		downStream.setInterpreter(interpreter);
		willReturn(downStream).given(consumerProducer).getCommand();
		willReturn(ExitStatus.success()).given(interpreter).eval(any(), any(), any(), any());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
	}

	@Test
	void recursiveWithExternalCommands() {
		// simulating a | b | c as external commands
		ExternalCommand a = mock(ExternalCommand.class, "a");
		ExternalCommand b = mock(ExternalCommand.class, "b");
		ExternalCommand c = mock(ExternalCommand.class, "c");
		PipelineCommand downStream = new PipelineCommand(new Statement(b, List.of(), ""), new Statement(c, List.of(), ""));
		PipelineCommand sut = new PipelineCommand(new Statement(a, List.of(), ""), new Statement(downStream, List.of(), ""));
		sut.setInterpreter(interpreter);
		downStream.setInterpreter(interpreter);
		given(interpreter.eval(any(), any(), any(), any())).willReturn(ExitStatus.success());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(a).should().pipeline(PipelineCommand.Position.FIRST);
		then(b).should().pipeline(PipelineCommand.Position.MIDDLE);
		then(c).should().pipeline(PipelineCommand.Position.LAST);
	}

}
