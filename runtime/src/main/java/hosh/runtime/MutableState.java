/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.runtime;

import hosh.doc.Todo;
import hosh.spi.Command;
import hosh.spi.CommandName;
import hosh.spi.State;
import hosh.spi.StateMutator;
import hosh.spi.VariableName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The state of the shell: it has been modeled as explicit state, in practice this is a global variable.
 */
@Todo(description = "NB: this is a tactical object used during migration to immutable state.")
public class MutableState implements State, StateMutator {

	// variables
	private volatile Map<VariableName, String> variables;

	// built-in commands
	private volatile Map<CommandName, Supplier<Command>> commands;

	// current working directory
	private volatile Path cwd;

	// where to look for commands (e.g. the "PATH" variable)
	private volatile List<Path> path;

	// request exit shell before executing next command
	private volatile boolean exit;

	public MutableState() {
		variables = Map.of();
		commands = Map.of();
		cwd = Paths.get("");
		path = List.of();
		exit = false;
	}

	@Override
	public Path getCwd() {
		return cwd;
	}

	@Override
	public Map<CommandName, Supplier<Command>> getCommands() {
		return commands;
	}

	@Override
	public Map<VariableName, String> getVariables() {
		return variables;
	}

	@Override
	public List<Path> getPath() {
		return path;
	}

	@Override
	public boolean isExit() {
		return exit;
	}

	@Override
	public void mutateCwd(Path newPath) {
		this.cwd = Objects.requireNonNull(newPath).normalize().toAbsolutePath();
	}

	@Override
	public void mutatePath(List<Path> newPath) {
		this.path = List.copyOf(Objects.requireNonNull(newPath));
	}

	@Override
	public void mutateExit(boolean newExit) {
		this.exit = newExit;
	}

	@Override
	public void mutateVariables(Map<VariableName, String> newVariables) {
		this.variables = Map.copyOf(Objects.requireNonNull(newVariables));
	}

	@Override
	public void mutateCommands(Map<CommandName, Supplier<Command>> newCommands) {
		this.commands = Map.copyOf(Objects.requireNonNull(newCommands));
	}

	@Override
	public String toString() {
		return String.format("MutableState[cwd='%s',path=%s,variables=%s,commands=%s]", cwd, path, variables, commands);
	}
}
