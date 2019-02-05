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
