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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
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
		List<CommandResolver> order = Arrays.asList(
				new BuiltinCommandResolver(state),
				new ExternalCommandResolver(state));
		return new AggregateCommandResolver(order);
	}

	public static class AggregateCommandResolver implements CommandResolver {
		private final List<CommandResolver> resolvers;

		public AggregateCommandResolver(List<CommandResolver> resolvers) {
			this.resolvers = resolvers;
		}

		@Override
		public Command tryResolve(String commandName) {
			for (CommandResolver resolver : resolvers) {
				Command resolved = resolver.tryResolve(commandName);
				if (resolved != null) {
					return resolved;
				}
			}
			return null;
		}
	}

	public static class ExternalCommandResolver implements CommandResolver {
		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
		private final State state;

		public ExternalCommandResolver(State state) {
			this.state = state;
		}

		@Override
		public Command tryResolve(String commandName) {
			LOGGER.info(() -> String.format("resolving commandName '%s' as system command", commandName));
			final Path candidate = Paths.get(commandName).normalize();
			if (candidate.isAbsolute() && Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
				LOGGER.info(() -> String.format("  found in %s", candidate));
				return new ExternalCommand(candidate);
			}
			for (Path dir : state.getPath()) {
				Path dirCandidate = Paths.get(dir.toString(), commandName).normalize();
				LOGGER.info(() -> String.format("  trying {}", dirCandidate));
				if (Files.isRegularFile(dirCandidate) && Files.isExecutable(dirCandidate)) {
					LOGGER.info(() -> String.format("  found in %s", dirCandidate));
					return new ExternalCommand(dirCandidate);
				}
			}
			LOGGER.info("  not found");
			return null;
		}
	}

	public static class BuiltinCommandResolver implements CommandResolver {
		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
		private final State state;

		public BuiltinCommandResolver(State state) {
			this.state = state;
		}

		@Override
		public Command tryResolve(String commandName) {
			LOGGER.info(() -> String.format("resolving commandName '%s' as builtin", commandName));
			Class<? extends Command> commandClass = state.getCommands().get(commandName);
			if (commandClass == null) {
				LOGGER.info("not found");
				return null;
			} else {
				LOGGER.info("found");
				return createInstance(commandClass);
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
}
