package org.hosh.spi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal state of the shell.
 */
public class State {

	// registered commands
	private final Map<String, Class<? extends Command>> commands = new HashMap<>();
	// current working directory
	private Path cwd;
	// current prompt
	private String prompt;

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

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	@Override
	public String toString() {
		return String.format("State[cwd='%s', prompt='%s', commands=%s]", cwd, prompt, commands);
	}
}
