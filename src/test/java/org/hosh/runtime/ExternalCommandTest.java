/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.junit.After;
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
	@Mock(name = "in")
	private Channel in;
	@Mock(name = "out")
	private Channel out;
	@Mock(name = "err")
	private Channel err;
	@Mock(stubOnly = true)
	private State state;
	@Mock(stubOnly = true)
	private Process process;
	@Mock
	private ProcessFactory processFactory;
	private Path executable;
	private ExternalCommand sut;

	@Before
	public void setup() throws IOException {
		executable = folder.newFile().toPath();
		sut = new ExternalCommand(executable);
		sut.setProcessFactory(processFactory);
		sut.setState(state);
	}

	@After
	public void after() {
		// this is needed since a test could set the current thread interrupted
		// otherwise random failures will be observed during the build
		Thread.interrupted();
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
		sut.run(Collections.emptyList(), in, out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString()),
				Paths.get("."),
				Collections.emptyMap(),
				true);
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
		sut.run(Collections.singletonList("file.hosh"), in, out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap(),
				true);
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
	}

	@Test
	public void processExitsWithError() throws Exception {
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(1);
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(process.getOutputStream()).willReturn(OutputStream.nullOutputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		ExitStatus exitStatus = sut.run(Collections.singletonList("file.hosh"), in, out, err);
		assertThat(exitStatus.isSuccess()).isFalse();
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap(),
				true);
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
		sut.run(Collections.singletonList("file.hosh"), in, out, err);
		then(processFactory).should().create(
				Arrays.asList(executable.toString(), "file.hosh"),
				Paths.get("."),
				Collections.emptyMap(),
				true);
		then(err).should().send(any());
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
		sut.run(Collections.singletonList("file.hosh"), in, out, err);
		then(out).should().send(Record.of("line", Values.ofText("test")));
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
		sut.run(Collections.singletonList("file.hosh"), in, out, err);
		then(out).shouldHaveZeroInteractions();
		then(err).should().send(Record.of("line", Values.ofText("test")));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void processRecordsFromIn() throws Exception {
		ByteArrayOutputStream value = new ByteArrayOutputStream();
		given(in.recv()).willReturn(
				Optional.of(Record.of("name", Values.ofText("aaa"), "size", Values.ofNumeric(10))),
				Optional.empty());
		given(processFactory.create(any(), any(), any(), any())).willReturn(process);
		given(process.waitFor()).willReturn(0);
		given(process.getOutputStream()).willReturn(value);
		given(process.getInputStream()).willReturn(InputStream.nullInputStream());
		given(process.getErrorStream()).willReturn(InputStream.nullInputStream());
		given(state.getCwd()).willReturn(Paths.get("."));
		given(state.getVariables()).willReturn(Collections.emptyMap());
		sut.run(Collections.singletonList("file.hosh"), in, out, err);
		then(out).shouldHaveZeroInteractions();
		then(err).shouldHaveZeroInteractions();
		assertThat(value.toString(StandardCharsets.UTF_8)).isEqualToNormalizingNewlines("aaa 10\n");
	}
}
