/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hosh.testsupport.ExitStatusAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.hosh.runtime.ExternalCommand.ProcessFactory;
import org.hosh.runtime.PipelineCommand.Position;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.hosh.testsupport.WithThread;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExternalCommandTest {

	@RegisterExtension
	public final WithThread withThread = new WithThread();

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	@Mock(stubOnly = true)
	private State state;

	@Mock(stubOnly = true)
	private Process process;

	@Mock
	private Channel in;

	@Mock
	private Channel out;

	@Mock
	private Channel err;

	@Mock
	private ProcessFactory processFactory;

	private Path executable;

	private ExternalCommand sut;

	@BeforeEach
	public void setup() throws IOException {
		executable = folder.newFile().toPath();
		sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
	}

	@Test
	public void processNoArgs() throws Exception {
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(process.getOutputStream()).willReturn(OutputStream.nullOutputStream());
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.emptyList(), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(processFactory).should().create(
				Arrays.asList(executable.toString()),
				Paths.get("."),
				Collections.emptyMap(),
				Position.SOLE);
		then(in).should().recv();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processWithArgs() throws Exception {
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(process.getOutputStream()).willReturn(OutputStream.nullOutputStream());
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap(),
				Position.SOLE);
		then(in).should().recv();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processExitsWithError() throws Exception {
		given(process.waitFor()).willReturn(1);
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(process.getOutputStream()).willReturn(OutputStream.nullOutputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus).isError();
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap(),
				Position.SOLE);
		then(in).should(times(1)).recv();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processException() throws Exception {
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willThrow(InterruptedException.class);
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(process.getOutputStream()).willReturn(OutputStream.nullOutputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus).isError();
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap(),
				Position.SOLE);
		then(in).should(times(1)).recv();
		then(out).shouldHaveZeroInteractions();
		then(err).should().send(Record.of(Keys.ERROR, Values.ofText("interrupted")));
	}

	@Test
	public void processSendRecordsToOut() throws Exception {
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(process.getOutputStream()).willReturn(OutputStream.nullOutputStream());
		given(process.getInputStream()).willReturn(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(in).should(times(1)).recv();
		then(out).should().send(Record.of(Keys.TEXT, Values.ofText("test")));
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processSendRecordsToErr() throws Exception {
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(process.getOutputStream()).willReturn(OutputStream.nullOutputStream());
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)));
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus).isSuccess();
		then(in).should(times(1)).recv();
		then(out).shouldHaveZeroInteractions();
		then(err).should().send(Record.of(Keys.TEXT, Values.ofText("test")));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void processRecordsFromIn() throws Exception {
		ByteArrayOutputStream value = new ByteArrayOutputStream();
		given(in.recv()).willReturn(
				Optional.of(Record.builder().entry(Keys.PATH, Values.ofText("aaa")).entry(Keys.SIZE, Values.ofNumeric(10)).build()),
				Optional.empty());
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(process.getOutputStream()).willReturn(value);
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus).isSuccess();
		assertThat(value.toString(StandardCharsets.UTF_8)).isEqualToNormalizingNewlines("aaa 10\n");
		then(in).should(times(2)).recv();
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void throwsIoException() throws Exception {
		given(processFactory.create(any(), any(), any(), any())).willThrow(new IOException("simulated error"));
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus).isError();
		then(in).shouldHaveZeroInteractions();
		then(out).shouldHaveZeroInteractions();
		then(err).should().send(Record.of(Keys.ERROR, Values.ofText("simulated error")));
	}
}
