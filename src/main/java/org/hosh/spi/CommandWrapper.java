package org.hosh.spi;

import java.util.List;

/**
 * A command specialization that performs setup and cleanup.
 * This is useful to create commands that locks a file.
 */
public interface CommandWrapper<T> extends Command {
	/**
	 * Create and setup a resource.
	 */
	T before(List<String> args, Channel out, Channel err);

	/**
	 * Finalize the resource.
	 */
	void after(T resource, Channel out, Channel err);
}
