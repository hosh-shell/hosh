package org.hosh.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalCommand implements Command, StateAware {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Path command;
	private ProcessFactory processFactory = new DefaultProcessFactory();
	private State state;
	private boolean inheritIo = true;

	public ExternalCommand(Path command) {
		this.command = command;
	}

	public void pipeline() {
		inheritIo = false;
	}

	@Todo(description = "redirecting err to out, this should be handled better")
	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		List<String> processArgs = new ArrayList<>(args.size() + 1);
		processArgs.add(command.toAbsolutePath().toString());
		processArgs.addAll(args);
		Path cwd = state.getCwd();
		logger.debug("executing command {} in directory {}", processArgs, cwd);
		try {
			Process process = processFactory.create(processArgs, cwd, state.getVariables(), inheritIo);
			int exitCode = process.waitFor();
			consumeStdout(out, process);
			consumeStderr(out, process);
			logger.debug("  exited with {}", exitCode);
			return ExitStatus.of(exitCode);
		} catch (IOException e) {
			logger.error("caught exception", e);
			err.send(Record.of("error", Values.ofText(e.getMessage())));
			return ExitStatus.error();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			err.send(Record.of("error", Values.ofText("interrupted")));
			return ExitStatus.error();
		}
	}

	private void consumeStdout(Channel out, Process process) throws IOException {
		consume(out, process.getInputStream());
	}

	private void consumeStderr(Channel err, Process process) throws IOException {
		consume(err, process.getErrorStream());
	}

	private void consume(Channel channel, InputStream inputStream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			while (true) {
				String readLine = reader.readLine();
				if (readLine == null) {
					break;
				}
				logger.debug("line: {}", readLine);
				boolean done = channel.trySend(Record.of("line", Values.ofText(readLine)));
				if (done) {
					break;
				}
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

	@Override
	public void setState(State state) {
		this.state = state;
	}

	// testing aid since we cannot mock ProcessBuilder
	interface ProcessFactory {
		Process create(List<String> args, Path cwd, Map<String, String> env, Boolean inheritIo) throws IOException;
	}

	public void setProcessFactory(ProcessFactory processFactory) {
		this.processFactory = processFactory;
	}
}
