package hosh.runtime;

import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static hosh.testsupport.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class LambdaCommandTest {

	@Mock
	private Interpreter interpreter;

	@Mock(stubOnly = true)
	private Compiler.Statement statement;

	@Mock(stubOnly = true)
	private InputChannel in;

	@Mock(stubOnly = true)
	private OutputChannel out;

	@Mock(stubOnly = true)
	private OutputChannel err;

	@Mock
	private State state;

	private LambdaCommand sut;

	@BeforeEach
	public void setUp() {
		sut = new LambdaCommand(statement, Keys.PATH.name());
		sut.setState(state);
		sut.setInterpreter(interpreter);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void success() {
		Map<String, String> variables = new HashMap<>();
		given(state.getVariables()).willReturn(variables);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.success());
		given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.PATH, Values.ofPath(Path.of("file")))), Optional.empty());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isSuccess();
		assertThat(variables).isEmpty();
		then(state).should().setVariables(Collections.singletonMap("path", "file"));
		then(state).should().setVariables(variables);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void error() {
		Map<String, String> variables = new HashMap<>();
		given(state.getVariables()).willReturn(variables);
		given(interpreter.eval(statement, in, out, err)).willReturn(ExitStatus.error());
		given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.PATH, Values.ofPath(Path.of("file")))), Optional.empty());
		ExitStatus exitStatus = sut.run(List.of(), in, out, err);
		assertThat(exitStatus).isError();
		assertThat(variables).isEmpty();
		then(state).should().setVariables(variables);
	}

}
