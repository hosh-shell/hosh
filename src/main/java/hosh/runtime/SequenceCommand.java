/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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

import java.util.List;

import hosh.runtime.Compiler.Statement;
import hosh.spi.Channel;
import hosh.spi.Command;
import hosh.spi.ExitStatus;

// generated by the compiler for 'cmd ; cmd'
public class SequenceCommand implements Command, InterpreterAware {

	private final Statement first;

	private final Statement second;

	private Interpreter interpreter;

	public SequenceCommand(Statement first, Statement second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public void setInterpreter(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
		ExitStatus exitStatus = interpreter.run(first, in, out, err);
		if (exitStatus.isError()) {
			return exitStatus;
		}
		return interpreter.run(second, in, out, err);
	}

	public Statement getFirst() {
		return first;
	}

	public Statement getSecond() {
		return second;
	}
}
