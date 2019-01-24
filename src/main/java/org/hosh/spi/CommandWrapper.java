package org.hosh.spi;

import java.util.List;

import org.hosh.doc.Todo;

/**
 * A command specialization that performs setup and cleanup.
 * 
 * This is useful to create resource-aware closures, such as locks,.
 */
public interface CommandWrapper<T> extends Command {
	@Todo(description = "dummy implementation: it looks like we have a design problem here?")
	@Override
	default ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		return ExitStatus.success();
	}

	/**
	 * Create and setup a resource.
	 */
	T before(List<String> args, Channel out, Channel err);

	/**
	 * Finalize the resource.
	 */
	void after(T resource, Channel out, Channel err);
}
