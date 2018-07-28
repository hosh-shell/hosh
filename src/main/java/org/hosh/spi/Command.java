package org.hosh.spi;

import java.util.List;

public interface Command {
	void run(List<String> args, Channel out, Channel err);
}
