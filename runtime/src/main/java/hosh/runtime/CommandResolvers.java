/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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

import hosh.spi.Command;
import hosh.spi.LoggerFactory;
import hosh.spi.State;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Various strategies to resolve a string
 * into a {@link Command} instance.
 */
public class CommandResolvers {

	private CommandResolvers() {
	}

	public static CommandResolver builtinsThenExternal(State state) {
		boolean isWindows = System.getProperty("os.name").startsWith("Windows");
		List<CommandResolver> order = new ArrayList<>();
		order.add(new BuiltinCommandResolver(state));
		order.add(new ExternalCommandResolver(state));
		if (isWindows) {
			order.add(new WindowsCommandResolver(state));
		}
		return new AggregateCommandResolver(order);
	}

	public static class AggregateCommandResolver implements CommandResolver {

		private final List<CommandResolver> resolvers;

		public AggregateCommandResolver(List<CommandResolver> resolvers) {
			this.resolvers = resolvers;
		}

		@Override
		public Optional<Command> tryResolve(String commandName) {
			for (CommandResolver resolver : resolvers) {
				Optional<Command> resolved = resolver.tryResolve(commandName);
				if (resolved.isPresent()) {
					return resolved;
				}
			}
			return Optional.empty();
		}
	}

	public static class ExternalCommandResolver implements CommandResolver {

		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

		private final State state;


		public ExternalCommandResolver(State state) {
			this.state = state;
		}

		@Override
		public Optional<Command> tryResolve(String commandName) {
			LOGGER.info(() -> String.format("resolving commandName '%s' as external command", commandName));
			final Path absoluteCandidate = Paths.get(commandName).normalize();
			if (absoluteCandidate.isAbsolute()) {
				return attemptResolution(absoluteCandidate);
			}
			List<Path> paths = new ArrayList<>(state.getPath());
			paths.add(state.getCwd());
			for (Path dir : paths) {
				Path candidate = Paths.get(dir.toString(), commandName).normalize();
				Optional<Command> command = attemptResolution(candidate);
				if (command.isPresent()) {
					return command;
				}
			}
			LOGGER.info("  not found");
			return Optional.empty();
		}

		private Optional<Command> attemptResolution(Path candidate) {
			LOGGER.info(() -> String.format("  trying %s", candidate));
			if (isExecutable(candidate)) {
				LOGGER.info(() -> String.format("  found in %s", candidate));
				ExternalCommand command = new ExternalCommand(candidate);
				return Optional.of(command);
			} else {
				return Optional.empty();
			}
		}

		private boolean isExecutable(Path candidate) {
			return Files.isRegularFile(candidate) && Files.isExecutable(candidate);
		}
	}

	public static class BuiltinCommandResolver implements CommandResolver {

		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

		private final State state;


		public BuiltinCommandResolver(State state) {
			this.state = state;
		}

		@Override
		public Optional<Command> tryResolve(String commandName) {
			LOGGER.info(() -> String.format("resolving commandName '%s' as built-in", commandName));
			Supplier<Command> commandSupplier = state.getCommands().get(commandName);
			if (commandSupplier == null) {
				LOGGER.info("  not found");
				return Optional.empty();
			} else {
				Command command = commandSupplier.get();
				LOGGER.info(() -> String.format("  found '%s'", command.getClass()));
				return Optional.of(command);
			}
		}
	}

	public static class WindowsCommandResolver implements CommandResolver {

		private final State state;

		private final ExternalCommandResolver resolver;

		public WindowsCommandResolver(State state) {
			this.state = state;
			resolver = new ExternalCommandResolver(state);
		}

		@Override
		public Optional<Command> tryResolve(String commandName) {
			String[] exts = state.getVariables().getOrDefault("PATHEXT", "").split(";", -1);
			for (String ext : exts) {
				Optional<Command> resolved = resolver.tryResolve(commandName + ext.strip());
				if (resolved.isPresent()) {
					return resolved;
				}
			}
			return Optional.empty();
		}
	}
}
