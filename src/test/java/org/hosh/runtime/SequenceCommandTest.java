package org.hosh.runtime;

import static org.hosh.testsupport.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
