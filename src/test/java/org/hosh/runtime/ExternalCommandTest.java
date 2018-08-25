package org.hosh.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.hosh.runtime.ExternalCommand.ProcessFactory;
import org.hosh.spi.Channel;
import org.hosh.spi.State;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ExternalCommandTest {
	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();
	@Mock(name = "out")
	private Channel out;
	@Mock(name = "err")
	private Channel err;
	@Mock
	private State state;
	@Mock
	private ProcessFactory processFactory;
	@Mock
	private Process process;
	private Path executable;

	@Before
	public void setup() throws IOException {
		executable = folder.newFile().toPath();
	}

	@Test
	public void processNoArgs() throws Exception {
		ExternalCommand sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.emptyList(), out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString()),
				Paths.get("."),
				Collections.emptyMap());
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processWithArgs() throws Exception {
		ExternalCommand sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap());
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processError() throws Exception {
		ExternalCommand sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willThrow(InterruptedException.class);
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap());
		then(out).shouldHaveZeroInteractions();
		then(err).should().send(any());
	}
}
