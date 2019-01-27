package org.hosh.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	public ExternalCommand(Path command) {
		this.command = command;
	}

	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Callable<ExitStatus> callable = () -> {
			List<String> processArgs = new ArrayList<>(args.size() + 1);
			processArgs.add(command.toAbsolutePath().toString());
			processArgs.addAll(args);
			Path cwd = state.getCwd();
			logger.debug("executing command {} in directory {}", processArgs, cwd);
			try {
				Process process = processFactory.create(processArgs, cwd, state.getVariables());
				int exitCode = process.waitFor();
				try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
					while (true) {
						String readLine = reader.readLine();
						if (readLine == null) {
							break;
						}
						logger.debug("line: {}", readLine);
						boolean done = out.trySend(Record.of("line", Values.ofText(readLine)));
						if (done) {
							break;
						}
					}
				}
				return ExitStatus.of(exitCode);
			} catch (IOException e) {
				err.send(Record.of("error", Values.ofText(e.getMessage())));
				logger.error("caught exception", e);
				return ExitStatus.error();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				err.send(Record.of("error", Values.ofText(e.getMessage())));
				return ExitStatus.error();
			}
		};
		Future<ExitStatus> submit = executor.submit(callable);
		try {
			ExitStatus exitStatus = submit.get();
			return exitStatus;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			logger.error("caught exception", e);
			return ExitStatus.error();
		} finally {
			executor.shutdownNow();
		}
	}

	private static class DefaultProcessFactory implements ProcessFactory {
		@Override
		public Process create(List<String> args, Path cwd, Map<String, String> env) throws IOException {
			ProcessBuilder processBuilder = new ProcessBuilder(args)
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
	interface ProcessFactory {
		Process create(List<String> args, Path cwd, Map<String, String> env) throws IOException;
	}

	public void setProcessFactory(ProcessFactory processFactory) {
		this.processFactory = processFactory;
	}
}
