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
	// current prompt
	private String prompt;

	public void setCwd(Path cwd) {
		this.cwd = cwd.normalize().toAbsolutePath();
	}

	public Path getCwd() {
		return cwd;
	}

	public Map<String, Command> getCommands() {
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
