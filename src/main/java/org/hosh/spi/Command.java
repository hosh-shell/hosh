package org.hosh.spi;

import java.util.List;

/**
 * Command represents a built-in (i.e. ls) or system commands (i.e. vim).
 */
public interface Command {
	ExitStatus run(List<String> args, Channel in, Channel out, Channel err);
}
