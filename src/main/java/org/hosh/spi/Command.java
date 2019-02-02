package org.hosh.spi;

import java.util.List;
import java.util.Optional;

import org.hosh.doc.Experimental;

/**
 * Command represents a built-in (i.e. ls) or system commands (i.e. vim).
 */
public interface Command {
	ExitStatus run(List<String> args, Channel in, Channel out, Channel err);

	default <T> Optional<T> downCast(Class<T> requiredClass) {
		if (requiredClass.isInstance(this)) {
			return Optional.of(requiredClass.cast(this));
		} else {
			return Optional.empty();
		}
	}

	@Experimental(description = "to inform the command that is a part of pipeline; "
			+ "this works because it is needed only by ExternalCommand and that is never shared (i.e. new instance)")
	default void pipeline() {
	}
}
