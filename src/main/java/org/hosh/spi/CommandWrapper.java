package org.hosh.spi;

import java.util.List;

/**
 * A command specialization that performs setup and cleanup.
 * This is useful to create commands that locks a file.
 */
public interface CommandWrapper extends Command {
	void before(List<String> args, Channel out, Channel err);

	void after(Channel out, Channel err);
}
