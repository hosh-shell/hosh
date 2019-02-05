package org.hosh.spi;

import java.util.List;

import org.hosh.doc.Todo;

/**
 * A command specialization that performs set-up and clean-up.
 */
public interface CommandWrapper<T> extends Command {
	/**
	 * Create and set-up a resource.
	 */
	T before(List<String> args, Channel in, Channel out, Channel err);

	/**
	 * Clean-up the resource.
	 */
	void after(T resource, Channel in, Channel out, Channel err);

	@Todo(description = "to remove this ugliness it is required to provide a better CommandRegistry")
	@Override
	default ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		throw new IllegalStateException("should be never called");
	}
}
