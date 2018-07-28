package org.hosh.spi;

import org.jline.terminal.Terminal;

/** Marker interface to inject Terminal in Commands */
public interface TerminalAware {
	void setTerminal(Terminal terminal);
}
