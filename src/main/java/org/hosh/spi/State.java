package org.hosh.spi;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * The state of the shell: it has been modeled as explicit state, this is
 * effective a global variable.
 * 
 * TODO: redesign to make dataflow unidirectional (like redux)
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

	public void setCwd(@Nonnull Path cwd) {
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

	public void setPrompt(@Nonnull String prompt) {
		this.prompt = prompt;
	}

	@Override
	public String toString() {
		return String.format("State[cwd='%s', prompt='%s', commands=%s]", cwd, prompt, commands);
	}
}
