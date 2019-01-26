package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.hosh.runtime.ExternalCommand.ProcessFactory;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
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
	@Mock(stubOnly = true)
	private State state;
	@Mock
	private ProcessFactory processFactory;
	@Mock(stubOnly = true)
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
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.emptyList(), null, out, err);
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
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), null, out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap());
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processExitsWithError() throws Exception {
		ExternalCommand sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(1);
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), null, out, err);
		assertThat(exitStatus.isSuccess()).isFalse();
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap());
	}

	@Test
	public void processException() throws Exception {
		ExternalCommand sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willThrow(InterruptedException.class);
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), null, out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap());
		then(err).should().send(any());
	}

	@Test
	public void processSendRecordsToOut() throws Exception {
		ExternalCommand sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
		given(processFactory.create(any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(process.getInputStream()).willReturn(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), null, out, err);
		then(out).should().trySend(Record.of("line", Values.ofText("test")));
		then(err).shouldHaveZeroInteractions();
	}
}
