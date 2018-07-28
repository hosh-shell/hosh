package org.hosh.runtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.hosh.runtime.ExternalCommand.ProcessFactory;
import org.hosh.spi.Channel;
import org.hosh.spi.State;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ExternalCommandTest {
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

	@Test
	public void processNoArgs() throws Exception {
		ExternalCommand sut = new ExternalCommand(Paths.get("/usr/bin/vim"));
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.emptyList(), out, err);
		then(processFactory).should().create(
				Arrays.asList("/usr/bin/vim"),
				Paths.get("."),
				Collections.emptyMap());
		then(out).should().send(any());
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processWithArgs() throws Exception {
		ExternalCommand sut = new ExternalCommand(Paths.get("/usr/bin/vim"));
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), out, err);
		then(processFactory).should().create(
				Arrays.asList("/usr/bin/vim", "file.hosh"),
				Paths.get("."),
				Collections.emptyMap());
		then(out).should().send(any());
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processError() throws Exception {
		ExternalCommand sut = new ExternalCommand(Paths.get("/usr/bin/vim"));
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willThrow(InterruptedException.class);
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), out, err);
		then(processFactory).should().create(
				Arrays.asList("/usr/bin/vim", "file.hosh"),
				Paths.get("."),
				Collections.emptyMap());
		then(out).shouldHaveZeroInteractions();
		then(err).should().send(any());
	}
}
