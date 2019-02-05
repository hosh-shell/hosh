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

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search a command using various strategies. This has been done to keep the
 * compiler unaware of how commands are resolved.
 */
public class CommandResolvers {
	private CommandResolvers() {
	}

	/**
	 * The sole strategy by now.
	 */
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
		private final Logger logger = LoggerFactory.getLogger(getClass());
		private final State state;

		public ExternalCommandResolver(State state) {
			this.state = state;
		}

		@Override
		public Command tryResolve(String commandName) {
			logger.info("resolving commandName '{}' as system command", commandName);
			Path candidate = Paths.get(commandName).normalize();
			if (candidate.isAbsolute() && Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
				logger.info("  found in {}", candidate);
				return new ExternalCommand(candidate);
			}
			for (Path dir : state.getPath()) {
				candidate = Paths.get(dir.toString(), commandName).normalize();
				logger.info("  trying {}", candidate);
				if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
					logger.info("  found in {}", candidate);
					return new ExternalCommand(candidate);
				}
			}
			logger.info("  not found");
			return null;
		}
	}

	public static class BuiltinCommandResolver implements CommandResolver {
		private final Logger logger = LoggerFactory.getLogger(getClass());
		private final State state;

		public BuiltinCommandResolver(State state) {
			this.state = state;
		}

		@Override
		public Command tryResolve(String commandName) {
			logger.info("resolving commandName '{}' as builtin", commandName);
			Command command = state.getCommands().get(commandName);
			logger.info("  {}", command != null ? "found" : "not found");
			return command;
		}
	}
}
