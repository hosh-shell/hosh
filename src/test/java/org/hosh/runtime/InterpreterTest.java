package org.hosh.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.List;

import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class InterpreterTest {

	@Mock
	private State state;

	@Mock
	private Terminal terminal;

	@Mock
	private Channel out;

	@Mock
	private Program program;

	@Mock
	private Statement statement;

	@Mock
	private List<String> args;

	@Mock
	private Command command;

	@Mock(extraInterfaces = StateAware.class)
	private Command stateAwareCommand;

	@Mock(extraInterfaces = TerminalAware.class)
	private Command terminalAwareCommand;

	@InjectMocks
	private Interpreter sut;

	@Test
	public void simpleCommand() throws Exception {
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);

		sut.eval(program);

		then(command).should().run(args, out, out);
		then(command).shouldHaveNoMoreInteractions();
	}

	@Test
	public void stateAwareCommand() throws Exception {
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(stateAwareCommand);
		given(statement.getArguments()).willReturn(args);

		sut.eval(program);

		then(stateAwareCommand).should().run(args, out, out);
		then((StateAware) stateAwareCommand).should().setState(state);
		then(stateAwareCommand).shouldHaveNoMoreInteractions();
	}

	@Test
	public void terminalAwareCommand() throws Exception {
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(terminalAwareCommand);
		given(statement.getArguments()).willReturn(args);

		sut.eval(program);

		then(terminalAwareCommand).should().run(args, out, out);
		then((TerminalAware) terminalAwareCommand).should().setTerminal(terminal);
		then(terminalAwareCommand).shouldHaveNoMoreInteractions();
	}

}
