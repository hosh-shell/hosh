package org.hosh.spi;

import java.util.List;

/**
 * A command specialization that performs set-up and clean-up.
 */
public interface CommandWrapper<T> extends Command {
	@Override
	default ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		return ExitStatus.success();
	}

	/**
	 * Create and set-up a resource.
	 */
	T before(List<String> args, Channel in, Channel out, Channel err);

	/**
	 * Clean-up the resource.
	 */
	void after(T resource, Channel in, Channel out, Channel err);
}
