/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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
package hosh.runtime;

import hosh.runtime.PipelineCommand.Position;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Values;

import java.io.BufferedReader;
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
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExternalCommand implements Command, StateAware {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private final Path path;

	private ProcessFactory processFactory = new DefaultProcessFactory();

	private State state; // needed for current working directory

	private PipelineCommand.Position position = Position.SOLE;

	public ExternalCommand(Path path) {
		this.path = path;
	}

	@Override
	public void setState(State state) {
		this.state = state;
	}

	public void pipeline(PipelineCommand.Position newPosition) {
		this.position = newPosition;
	}

	@Override
	public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
		List<String> processArgs = new ArrayList<>(args.size() + 1);
		processArgs.add(path.toAbsolutePath().toString());
		processArgs.addAll(args);
		Path cwd = state.getCwd();
		LOGGER.fine(() -> String.format("executing '%s' in directory %s", processArgs, cwd));
		LOGGER.fine(() -> String.format("in '%s', out '%s', err '%s'", in, out, err));
		try {
			Process process = processFactory.create(processArgs, cwd, state.getVariables(), position);
			writeStdin(in, process);
			readStdout(out, process);
			readStderr(err, process);
			int exitCode = process.waitFor();
			LOGGER.fine(() -> String.format("exited with %s", exitCode));
			return ExitStatus.of(exitCode);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "caught exception", e);
			err.send(Records.singleton(Keys.ERROR, Values.ofText(e.getMessage())));
			return ExitStatus.error();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			err.send(Records.singleton(Keys.ERROR, Values.ofText("interrupted")));
			return ExitStatus.error();
		}
	}

	private void writeStdin(InputChannel in, Process process) {
		pipeChannelToOutputStream(in, process.getOutputStream());
	}

	private void pipeChannelToOutputStream(InputChannel in, OutputStream outputStream) {
		Locale locale = Locale.getDefault();
		try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
			for (Record record : InputChannel.iterate(in)) {
				record.print(pw, locale);
				pw.println();
			}
		}
	}

	private void readStdout(OutputChannel out, Process process) throws IOException {
		pipeInputStreamToChannel(out, process.getInputStream());
	}

	private void readStderr(OutputChannel err, Process process) throws IOException {
		pipeInputStreamToChannel(err, process.getErrorStream());
	}

	private void pipeInputStreamToChannel(OutputChannel channel, InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			while (true) {
				String readLine = reader.readLine();
				if (readLine == null) {
					break;
				}
				channel.send(Records.singleton(Keys.TEXT, Values.ofText(readLine)));
			}
		}
	}

	// testing aid since we cannot mock ProcessBuilder
	interface ProcessFactory {

		Process create(List<String> args, Path cwd, Map<String, String> env, PipelineCommand.Position position) throws IOException;
	}

	private static class DefaultProcessFactory implements ProcessFactory {

		@Override
		public Process create(List<String> args, Path cwd, Map<String, String> env, PipelineCommand.Position position) throws IOException {
			ProcessBuilder processBuilder = new ProcessBuilder(args)
				                                .directory(cwd.toFile());
			processBuilder.environment().clear();
			processBuilder.environment().putAll(env);
			processBuilder.inheritIO();
			if (position.redirectInput()) {
				LOGGER.fine("setting PIPE for input");
				processBuilder.redirectInput(Redirect.PIPE);
			}
			if (position.redirectOutput()) {
				LOGGER.fine("setting PIPE for output");
				processBuilder.redirectOutput(Redirect.PIPE);
			}
			return processBuilder.start();
		}
	}

	public void setProcessFactory(ProcessFactory processFactory) {
		this.processFactory = processFactory;
	}
}
