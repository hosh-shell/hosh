/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
	// request exit shell before executing next command
	private boolean exit = false;

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

	public boolean isExit() {
		return exit;
	}

	public void setExit(boolean exit) {
		this.exit = exit;
	}

	@Override
	public String toString() {
		return String.format("State[cwd='%s',id=%s,path=%s,variables=%s,commands=%s]", cwd, id, path, variables, commands);
	}
}
