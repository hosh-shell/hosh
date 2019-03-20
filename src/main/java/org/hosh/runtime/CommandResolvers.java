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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.hosh.spi.Command;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.State;

/**
 * Search a command using various strategies. This has been done to keep the
 * compiler unaware of how commands are resolved.
 */
public class CommandResolvers {

	private CommandResolvers() {
	}

	public static CommandResolver builtinsThenSystem(State state) {
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
			LOGGER.info(() -> String.format("resolving commandName '%s' as system command", commandName));
			final Path absoluteCandidate = Paths.get(commandName).normalize();
			if (absoluteCandidate.isAbsolute()) {
				Optional<Command> command = attemptResolution(absoluteCandidate);
				if (command.isPresent()) {
					return command;
				}
				return Optional.empty();
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
				return Optional.of(new ExternalCommand(candidate));
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
			Class<? extends Command> commandClass = state.getCommands().get(commandName);
			if (commandClass == null) {
				LOGGER.info("  not found");
				return Optional.empty();
			} else {
				LOGGER.info(() -> String.format("  found '%s'", commandClass));
				return Optional.of(createInstance(commandClass));
			}
		}

		private Command createInstance(Class<? extends Command> commandClass) {
			try {
				return commandClass.getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new IllegalArgumentException("cannot create instance of " + commandClass, e);
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
			String[] exts = state.getVariables().getOrDefault("PATHEXT", "").split(";");
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
