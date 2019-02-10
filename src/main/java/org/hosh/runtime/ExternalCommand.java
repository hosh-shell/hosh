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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Values;

public class ExternalCommand implements Command, StateAware {
	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private final Path command;
	private ProcessFactory processFactory = new DefaultProcessFactory();
	private State state; // needed for current working directory
	private boolean inheritIo = true;

	public ExternalCommand(Path command) {
		this.command = command;
	}

	@Override
	public void setState(State state) {
		this.state = state;
	}

	public void pipeline() {
		inheritIo = false;
	}

	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		List<String> processArgs = new ArrayList<>(args.size() + 1);
		processArgs.add(command.toAbsolutePath().toString());
		processArgs.addAll(args);
		Path cwd = state.getCwd();
		LOGGER.fine(() -> String.format("executing '%s' in directory %s", processArgs, cwd));
		LOGGER.fine(() -> String.format("in '%s', out '%s', err '%s'", in, out, err));
		try {
			Process process = processFactory.create(processArgs, cwd, state.getVariables(), inheritIo);
			writeStdin(in, process);
			readStdout(out, process);
			readStderr(err, process);
			int exitCode = process.waitFor();
			LOGGER.fine(() -> String.format("exited with %s", exitCode));
			return ExitStatus.of(exitCode);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "caught exception", e);
			err.send(Record.of("error", Values.ofText(e.getMessage())));
			return ExitStatus.error();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			err.send(Record.of("error", Values.ofText("interrupted")));
			return ExitStatus.error();
		}
	}

	private void writeStdin(Channel in, Process process) {
		pipeChannelToOutputStream(in, process.getOutputStream());
	}

	private void pipeChannelToOutputStream(Channel in, OutputStream outputStream) {
		try (PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)))) {
			RecordWriter recordWriter = new RecordWriter(pw, Ansi.Style.NONE);
			while (true) {
				Optional<Record> recv = in.recv();
				if (recv.isEmpty()) {
					break;
				}
				Record record = recv.get();
				recordWriter.writeValues(record);
			}
		}
	}

	private void readStdout(Channel out, Process process) throws IOException {
		pipeInputStreamToChannel(out, process.getInputStream());
	}

	private void readStderr(Channel err, Process process) throws IOException {
		pipeInputStreamToChannel(err, process.getErrorStream());
	}

	private void pipeInputStreamToChannel(Channel channel, InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			while (true) {
				String readLine = reader.readLine();
				if (readLine == null) {
					break;
				}
				channel.send(Record.of("line", Values.ofText(readLine)));
			}
		}
	}

	private static class DefaultProcessFactory implements ProcessFactory {
		@Override
		public Process create(List<String> args, Path cwd, Map<String, String> env, Boolean inheritIo) throws IOException {
			ProcessBuilder processBuilder = new ProcessBuilder(args)
					.directory(cwd.toFile());
			if (inheritIo) {
				processBuilder.redirectInput(Redirect.INHERIT);
				processBuilder.redirectOutput(Redirect.INHERIT);
				processBuilder.redirectError(Redirect.PIPE);
			}
			processBuilder.environment().putAll(env);
			return processBuilder.start();
		}
	}

	// testing aid since we cannot mock ProcessBuilder
	interface ProcessFactory {
		Process create(List<String> args, Path cwd, Map<String, String> env, Boolean inheritIo) throws IOException;
	}

	public void setProcessFactory(ProcessFactory processFactory) {
		this.processFactory = processFactory;
	}
}
