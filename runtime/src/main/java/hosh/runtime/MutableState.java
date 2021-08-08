/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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

import hosh.spi.Command;
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
 *
 * NB: this is a tactical object used during migration to immutable state.
 */
public class MutableState implements State, StateMutator {

	// variables
	private Map<VariableName, String> variables = Map.of();

	// built-in commands
	private Map<String, Supplier<Command>> commands = Map.of();

	// current working directory
	private Path cwd = Paths.get("");

	// PATH
	private List<Path> path = List.of();

	// request exit shell before executing next command
	private boolean exit = false;

	@Override
	public Path getCwd() {
		return cwd;
	}

	@Override
	public Map<String, Supplier<Command>> getCommands() {
		// defensive programming: creating an unmodifiable list to spot modifications via reference
		return Map.copyOf(commands);
	}

	@Override
	public Map<VariableName, String> getVariables() {
		// defensive programming: creating an unmodifiable list to spot modifications via reference
		return Map.copyOf(variables);
	}

	@Override
	public List<Path> getPath() {
		// defensive programming: creating an unmodifiable list to spot modifications via reference
		return List.copyOf(path);
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
		this.path = Objects.requireNonNull(newPath);
	}

	@Override
	public void mutateExit(boolean newExit) {
		this.exit = newExit;
	}

	@Override
	public void mutateVariables(Map<VariableName, String> newVariables) {
		this.variables = Objects.requireNonNull(newVariables);
	}

	@Override
	public void mutateCommands(Map<String, Supplier<Command>> newCommands) {
		this.commands = Objects.requireNonNull(newCommands);
	}

	@Override
	public String toString() {
		return String.format("MutableState[cwd='%s',path=%s,variables=%s,commands=%s]", cwd, path, variables, commands);
	}
}
