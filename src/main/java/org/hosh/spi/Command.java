package org.hosh.spi;

import java.util.List;

import org.hosh.doc.Experimental;

/**
 * Command represents a built-in (i.e. ls) or system commands (i.e. vim).
 */
public interface Command {
	ExitStatus run(List<String> args, Channel in, Channel out, Channel err);

	@Experimental(description = "to inform the command that is a part of pipeline; "
			+ "this implies that Hosh should register command class and create new instance from scrarch")
	default void pipeline() {
	}
}
