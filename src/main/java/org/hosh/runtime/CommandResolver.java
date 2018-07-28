package org.hosh.runtime;

import org.hosh.doc.Todo;
import org.hosh.spi.Command;

@Todo(description = "Perhaps Optional would be a better choice here?")
public interface CommandResolver {
	/** Yields null if not found. */
	Command tryResolve(String commandName);
}