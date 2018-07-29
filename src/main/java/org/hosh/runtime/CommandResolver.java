package org.hosh.runtime;

import org.hosh.spi.Command;

public interface CommandResolver {
	/** Yields null if not found. */
	Command tryResolve(String commandName);
}