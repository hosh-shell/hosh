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

import java.util.List;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandWrapper;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;

public class DefaultCommandWrapper<T> implements Command, StateAware, TerminalAware, ArgumentResolverAware, SupervisorAware {
	private final Statement nested;
	private final CommandWrapper<T> commandWrapper;
	private ArgumentResolver argumentResolver;

	public DefaultCommandWrapper(Statement nested, CommandWrapper<T> commandWrapper) {
		this.nested = nested;
		this.commandWrapper = commandWrapper;
	}

	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		T resource = commandWrapper.before(args, in, out, err);
		try {
			while (true) {
				List<String> resolvedArguments = resolveArguments();
				ExitStatus exitStatus = nested.getCommand().run(resolvedArguments, in, out, err);
				if (commandWrapper.retry(resource)) {
					continue;
				}
				return exitStatus;
			}
		} finally {
			commandWrapper.after(resource, in, out, err);
		}
	}

	private List<String> resolveArguments() {
		List<String> arguments = nested.getArguments();
		List<String> resolvedArguments = argumentResolver.resolve(arguments);
		return resolvedArguments;
	}

	@Override
	public String toString() {
		return String.format("DefaultCommandWrapper[nested=%s,commandWrapper=%s]", nested, commandWrapper);
	}

	@Override
	public void setState(State state) {
		nested.getCommand().downCast(StateAware.class).ifPresent(cmd -> cmd.setState(state));
	}

	@Override
	public void setTerminal(Terminal terminal) {
		nested.getCommand().downCast(TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
	}

	@Override
	public void setArgumentResolver(ArgumentResolver argumentResolver) {
		nested.getCommand().downCast(ArgumentResolverAware.class).ifPresent(cmd -> cmd.setArgumentResolver(argumentResolver));
		this.argumentResolver = argumentResolver;
	}

	@Override
	public void setSupervisor(Supervisor supervisor) {
		nested.getCommand().downCast(SupervisorAware.class).ifPresent(cmd -> cmd.setSupervisor(supervisor));
	}
}
