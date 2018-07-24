package org.hosh.runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalCommand implements Command, StateAware {

	private static final Logger logger = LoggerFactory.getLogger(ExternalCommand.class);

	private final Path command;
	private ProcessFactory processFactory = new DefaultProcessFactory();
	private State state;

	public ExternalCommand(Path command) {
		this.command = command;
	}

	@Todo(description = "save exitCode as variable?")
	@Override
	public void run(List<String> args, Channel out, Channel err) {
		List<String> processArgs = new ArrayList<>(args.size() + 1);
		processArgs.add(command.toAbsolutePath().toString());
		processArgs.addAll(args);
		Path cwd = state.getCwd();
		logger.info("executing command {} inside {}", processArgs, cwd);
		try {
			Process process = processFactory.create(processArgs, cwd, state.getVariables());
			int exitCode = process.waitFor();
			out.send(Record.of("message", Values.ofText("exit code: " + exitCode)));
		} catch (IOException | InterruptedException e) {
			err.send(Record.of("error", Values.ofText(e.getMessage())));
			logger.error("caught exception", e);
		}
	}

	private static class DefaultProcessFactory implements ProcessFactory {

		@Override
		public Process create(List<String> args, Path cwd, Map<String, String> env) throws IOException {
			ProcessBuilder processBuilder = new ProcessBuilder(args.toArray(new String[0]))
					.directory(cwd.toFile())
					.inheritIO();
			processBuilder.environment().putAll(env);
			return processBuilder.start();
		}

	}

	@Override
	public void setState(State state) {
		this.state = state;
	}

	// testing aid since we cannot mock ProcessBuilder
	static interface ProcessFactory {

		Process create(List<String> args, Path cwd, Map<String, String> env) throws IOException;

	}

	public void setProcessFactory(ProcessFactory processFactory) {
		this.processFactory = processFactory;
	}

}