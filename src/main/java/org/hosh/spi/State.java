package org.hosh.spi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal state of the shell.
 * 
 * TODO: add prompt
 */
public class State {

	private final Map<String, Class<? extends Command>> commands = new HashMap<>();

	// current working directory
	private Path cwd;

	public State() {
		setCwd(Paths.get("."));
	}

	public void setCwd(Path cwd) {
		this.cwd = cwd.normalize().toAbsolutePath();
	}

	public Path getCwd() {
		return cwd;
	}

	public Map<String, Class<? extends Command>> getCommands() {
		return commands;
	}

	@Override
	public String toString() {
		return String.format("State[cwd=%s, commands=%s]", cwd, commands);
	}
}
