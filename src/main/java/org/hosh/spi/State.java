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

	private final Map<String, Command> commands = new HashMap<>();
	private final Map<String, Module> modules = new HashMap<>();

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

	public Map<String, Command> getCommands() {
		return commands;
	}

	public Map<String, Module> getModules() {
		return modules;
	}
}
