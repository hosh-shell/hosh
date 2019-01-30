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

public class DefaultCommandWrapper<T> implements Command, StateAware, TerminalAware {
	private final Statement nestedStatement;
	private final CommandWrapper<T> commandWrapper;

	public DefaultCommandWrapper(Statement nestedStatement, CommandWrapper<T> commandWrapper) {
		this.nestedStatement = nestedStatement;
		this.commandWrapper = commandWrapper;
	}

	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		T resource = commandWrapper.before(args, in, out, err);
		try {
			return nestedStatement.getCommand().run(nestedStatement.getArguments(), in, out, err);
		} finally {
			commandWrapper.after(resource, in, out, err);
		}
	}

	@Override
	public String toString() {
		return String.format("DefaultCommandWrapper[nested=%s,wrapper=%s]", nestedStatement, commandWrapper);
	}

	@Override
	public void setState(State state) {
		Command command = nestedStatement.getCommand();
		if (command instanceof StateAware) {
			((StateAware) command).setState(state);
		}
	}

	@Override
	public void setTerminal(Terminal terminal) {
		Command command = nestedStatement.getCommand();
		if (command instanceof TerminalAware) {
			((TerminalAware) command).setTerminal(terminal);
		}
	}
}
