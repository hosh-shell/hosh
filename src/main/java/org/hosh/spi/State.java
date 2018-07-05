package org.hosh.spi;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The state of the shell: it has been modeled as explicit state, this is
 * effective a global variable.
 */
public class State {

	// registered commands
	private final Map<String, Command> commands = new HashMap<>();
	// current working directory
	private Path cwd;
	// current command id: used by the REPL
	private int id;

	public void setCwd(Path cwd) {
		this.cwd = cwd.normalize().toAbsolutePath();
	}

	public Path getCwd() {
		return cwd;
	}

	public Map<String, Command> getCommands() {
		return commands;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return String.format("State[cwd='%s',id=%s,commands=%s]", cwd, id, commands);
	}

}
