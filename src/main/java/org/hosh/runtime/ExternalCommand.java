package org.hosh.runtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Value;
import org.hosh.spi.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalCommand implements Command, StateAware {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalCommand.class);
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
		LOGGER.debug("external: executing '{}' in directory {}", processArgs, cwd);
		try {
			Process process = processFactory.create(processArgs, cwd, state.getVariables(), inheritIo);
			writeStdin(in, process);
			readStdout(out, process);
			readStderr(err, process);
			int exitCode = process.waitFor();
			LOGGER.debug("external: exited with {}", exitCode);
			return ExitStatus.of(exitCode);
		} catch (IOException e) {
			LOGGER.error("external: caught exception", e);
			err.send(Record.of("error", Values.ofText(e.getMessage())));
			return ExitStatus.error();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			err.send(Record.of("error", Values.ofText("interrupted")));
			return ExitStatus.error();
		}
	}

	private void writeStdin(Channel in, Process process) throws IOException {
		pipeChannelToOutputStream(in, process.getOutputStream());
	}

	private void pipeChannelToOutputStream(Channel in, OutputStream outputStream) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new java.io.OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
			while (true) {
				Optional<Record> recv = in.recv();
				if (recv.isEmpty()) {
					break;
				}
				Record record = recv.get();
				LOGGER.debug("external: got record '{}'", record);
				writeAsLine(writer, record);
			}
		}
	}

	@Todo(description = "users cannot change separator by now")
	private void writeAsLine(BufferedWriter writer, Record record) throws IOException {
		Iterator<Value> iterator = record.values().iterator();
		while (iterator.hasNext()) {
			Value value = iterator.next();
			value.append(writer, Locale.getDefault());
			if (iterator.hasNext()) {
				writer.append(' ');
			}
		}
		writer.newLine();
	}

	private void readStdout(Channel out, Process process) throws IOException {
		pipeInputStreamToChannel(out, process.getInputStream());
	}

	private void readStderr(Channel err, Process process) throws IOException {
		pipeInputStreamToChannel(err, process.getErrorStream());
	}

	private void pipeInputStreamToChannel(Channel channel, InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			while (true) {
				String readLine = reader.readLine();
				if (readLine == null) {
					break;
				}
				LOGGER.debug("external: got line '{}'", readLine);
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
				processBuilder.inheritIO();
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
