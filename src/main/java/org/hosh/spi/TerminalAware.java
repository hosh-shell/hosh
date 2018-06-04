package org.hosh.spi;

import javax.annotation.Nonnull;

import org.jline.terminal.Terminal;

/** Marker interface to inject Terminal in Commands */
public interface TerminalAware {

	void setTerminal(@Nonnull Terminal terminal);

}
