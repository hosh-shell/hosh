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
import hosh.spi.Value;
import hosh.spi.VariableName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The state of the shell: it has been modeled as explicit state, in practice this is a global variable.
 */
@Todo(description = "NB: this is a tactical object used during migration to immutable state.")
public class MutableState implements State, StateMutator {

	// variables
	private final AtomicReference<Map<VariableName, Value>> variables;

	// built-in commands
	private final AtomicReference<Map<CommandName, Supplier<Command>>> commands;

	// current working directory
	private final AtomicReference<Path> cwd;

	// where to look for external commands (e.g. the "PATH" variable)
	private final AtomicReference<List<Path>> path;

	// request exit shell before executing next command
	private final AtomicBoolean exit;

	public MutableState() {
		variables = new AtomicReference<>(Map.of());
		commands = new AtomicReference<>(Map.of());
		cwd = new AtomicReference<>(Paths.get(""));
		path = new AtomicReference<>(List.of());
		exit = new AtomicBoolean(false);
	}

	@Override
	public Path getCwd() {
		return cwd.get();
	}

	@Override
	public Map<CommandName, Supplier<Command>> getCommands() {
		return commands.get();
	}

	@Override
	public Map<VariableName, Value> getVariables() {
		return variables.get();
	}

	@Override
	public List<Path> getPath() {
		return path.get();
	}

	@Override
	public boolean isExit() {
		return exit.get();
	}

	@Override
	public void mutateCwd(Path newPath) {
		cwd.set(Objects.requireNonNull(newPath).normalize().toAbsolutePath());
	}

	@Override
	public void mutatePath(List<Path> newPath) {
		path.set(List.copyOf(Objects.requireNonNull(newPath)));
	}

	@Override
	public void mutateExit(boolean newExit) {
		exit.set(newExit);
	}

	@Override
	public void mutateVariables(Map<VariableName, Value> newVariables) {
		variables.set(Map.copyOf(Objects.requireNonNull(newVariables)));
	}

	@Override
	public void mutateCommands(Map<CommandName, Supplier<Command>> newCommands) {
		commands.set(Map.copyOf(Objects.requireNonNull(newCommands)));
	}

	@Override
	public String toString() {
		return String.format("MutableState[cwd='%s',path=%s,variables=%s,commands=%s]", cwd.get(), path.get(), variables.get(), commands.get());
	}
}
