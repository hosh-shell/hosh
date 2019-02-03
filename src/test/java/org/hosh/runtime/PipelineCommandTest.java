package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
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
	@Mock
	private Terminal terminal;
	@Mock
	private Command command1;
	@Mock
	private Command command2;

	private PipelineCommand setupSut() {
		Statement producer = new Statement();
		producer.setCommand(command1);
		producer.setArguments(Collections.emptyList());
		Statement consumer = new Statement();
		consumer.setCommand(command2);
		consumer.setArguments(Collections.emptyList());
		PipelineCommand sut = new PipelineCommand(List.of(producer, consumer));
		sut.setTerminal(terminal);
		return sut;
	}

	@Test
	public void producerAndConsumerSuccess() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(true);
	}

	@Test
	public void producerFailure() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.error());
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void consumerFailure() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.error());
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void producerInterrupted() {
		given(command1.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willThrow(NullPointerException.class);
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}

	@Test
	public void consumerInterrupted() {
		given(command2.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).willThrow(NullPointerException.class);
		PipelineCommand sut = setupSut();
		ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
		assertThat(exitStatus.isSuccess()).isEqualTo(false);
	}
}
