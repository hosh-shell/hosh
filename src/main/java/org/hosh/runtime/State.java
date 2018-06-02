package org.hosh.runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.hosh.spi.Command;
import org.hosh.spi.Module;

/**
 * Internal state of the shell.
 * 
 * - can be read freely - can be modified only via controlled side-effecting
 * actions
 *
 * The state can be passed freely to components: - modules (e.g. help module)
 * can use to produce some documentation or to change current working directory
 * - completer can use it to help the user to autocomplete commands - to
 * distribute read-only configuration to every component
 */
@SuppressWarnings("unused")
public class State {

	// version of hosh, cannot be change
	private final String version = "1.0";

	// current working directory
	private final Path cwd = Paths.get(".");

	private final Map<String, Command> commands = new HashMap<>();
	private final Map<String, Module> modules = new HashMap<>();

}
