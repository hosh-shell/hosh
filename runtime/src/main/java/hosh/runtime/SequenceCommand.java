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
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;

import java.util.List;

// generated by the compiler for 'cmd ; cmd'
class SequenceCommand implements Command, InterpreterAware {

	private final Compiler.Statement first;

	private final Compiler.Statement second;

	private Interpreter interpreter;

	public SequenceCommand(Compiler.Statement first, Compiler.Statement second) {
		this.first = first;
		this.second = second;
	}

	@Override
	public void setInterpreter(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
		ExitStatus exitStatus = interpreter.eval(first, in, out, err);
		if (exitStatus.isError()) {
			return exitStatus;
		}
		return interpreter.eval(second, in, out, err);
	}

	public Compiler.Statement getFirst() {
		return first;
	}

	public Compiler.Statement getSecond() {
		return second;
	}
}
