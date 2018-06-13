package org.hosh.runtime;

import java.util.Objects;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.TerminalAware;
import org.jline.terminal.Terminal;

public class CommandFactory {

	private final Terminal terminal;
	private final State state;
	private Strategy strategy = new ReflectionStrategy();

	public CommandFactory(
			State state, Terminal terminal) {
		this.state = state;
		this.terminal = terminal;
	}

	public Command create(Class<? extends Command> commandClass) {
		Command command = strategy.createCommand(commandClass);
		handleTerminalAware(command);
		handleStateAware(command);
		return command;
	}

	private void handleStateAware(Command command) {
		if (command instanceof StateAware) {
			((StateAware) command).setState(state);
		}
	}

	private void handleTerminalAware(Command command) {
		if (command instanceof TerminalAware) {
			((TerminalAware) command).setTerminal(terminal);
		}
	}

	public void setStrategy(Strategy newStrategy) {
		this.strategy = Objects.requireNonNull(newStrategy);
	}

	@FunctionalInterface
	public static interface Strategy {

		Command createCommand(Class<? extends Command> commandClass);

	}

	private static class ReflectionStrategy implements Strategy {

		@Override
		public Command createCommand(Class<? extends Command> commandClass) {
			try {
				return commandClass.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException ex) {
				throw new IllegalArgumentException("cannot instantiate command using default empty costructor", ex);
			}
		}
	}

}
