package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class InterpreterTest {
	private Map<String, String> variables = new HashMap<>();
	private List<String> args = new ArrayList<>();
	@Mock(stubOnly = true)
	private State state;
	@Mock(stubOnly = true)
	private Terminal terminal;
	@Mock(name = "out", stubOnly = true)
	private Channel out;
	@Mock(stubOnly = true)
	private Program program;
	@Mock(stubOnly = true)
	private Statement statement;
	@Mock
	private Command command;
	@Mock(extraInterfaces = StateAware.class)
	private Command stateAwareCommand;
	@Mock(extraInterfaces = TerminalAware.class)
	private Command terminalAwareCommand;
	@InjectMocks
	private Interpreter sut;

	@Test
	public void plainCommandWithoutDeps() throws Exception {
		given(state.getVariables()).willReturn(variables);
		given(command.run(args, out, out)).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(command).should().run(args, out, out);
		then(command).shouldHaveNoMoreInteractions();
	}

	@Test
	public void storeCommandExitStatus() throws Exception {
		given(state.getVariables()).willReturn(variables);
		given(command.run(args, out, out)).willReturn(ExitStatus.error());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(command).should().run(args, out, out);
		then(command).shouldHaveNoMoreInteractions();
		assertThat(variables).containsEntry("EXIT_STATUS", "1");
	}

	@Test
	public void stateAwareCommand() throws Exception {
		given(state.getVariables()).willReturn(variables);
		given(stateAwareCommand.run(args, out, out)).willReturn(ExitStatus.success());
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
		given(terminalAwareCommand.run(args, out, out)).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(terminalAwareCommand);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(terminalAwareCommand).should().run(args, out, out);
		then((TerminalAware) terminalAwareCommand).should().setTerminal(terminal);
		then(terminalAwareCommand).shouldHaveNoMoreInteractions();
	}

	@Test
	public void plainArguments() throws Exception {
		args.add("file");
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(command).should().run(Arrays.asList("file"), out, out);
		then(command).shouldHaveNoMoreInteractions();
	}

	@Test
	public void resolvePresentVariables() throws Exception {
		args.add("${VARIABLE}");
		variables.put("VARIABLE", "1");
		given(state.getVariables()).willReturn(variables);
		given(command.run(Mockito.any(), Mockito.any(), Mockito.any())).willReturn(ExitStatus.success());
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		sut.eval(program);
		then(command).should().run(Arrays.asList("1"), out, out);
		then(command).shouldHaveNoMoreInteractions();
	}

	@Test
	public void refuseAbsentVariables() throws Exception {
		args.add("${VARIABLE}");
		given(state.getVariables()).willReturn(variables);
		given(program.getStatements()).willReturn(Arrays.asList(statement));
		given(statement.getCommand()).willReturn(command);
		given(statement.getArguments()).willReturn(args);
		assertThatThrownBy(() -> sut.eval(program)).isInstanceOf(IllegalStateException.class).hasMessageContaining("unknown variable: VARIABLE");
	}
}
