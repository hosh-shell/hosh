package org.hosh.spi;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The state of the shell: it has been modeled as explicit state,
 * in practice this is a global variable.
 */
public class State {
	// variables
	private final Map<String, String> variables = new HashMap<>();
	// registered commands
	private final Map<String, Command> commands = new HashMap<>();
	// current working directory
	private Path cwd;
	// current command id: used by the REPL
	private int id;
	// PATH
	private List<Path> path = new ArrayList<>();

	public void setCwd(Path cwd) {
		this.cwd = cwd.normalize().toAbsolutePath();
	}

	public Path getCwd() {
		return cwd;
	}

	public Map<String, Command> getCommands() {
		return commands;
	}

	public Map<String, String> getVariables() {
		return variables;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public List<Path> getPath() {
		return path;
	}

	public void setPath(List<Path> path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return String.format("State[cwd='%s',id=%s,path=%s,variables=%s,commands=%s]", cwd, id, path, variables, commands);
	}
}
