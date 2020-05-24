/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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
package runtime;

import runtime.Compiler.Statement;
import hosh.spi.Command;
import hosh.spi.CommandWrapper;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;

import java.util.List;

// generated by compiler for 'cmd { ... }'
public class DefaultCommandWrapper<T> implements Command, InterpreterAware {

	private final Statement nested;

	private final CommandWrapper<T> commandWrapper;

	private Interpreter interpreter;

	public DefaultCommandWrapper(Statement nested, CommandWrapper<T> commandWrapper) {
		this.nested = nested;
		this.commandWrapper = commandWrapper;
	}

	@Override
	public void setInterpreter(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
		T resource = commandWrapper.before(args, in, out, err);
		try {
			while (true) {
				ExitStatus exitStatus = interpreter.eval(nested, in, out, err);
				boolean retry = commandWrapper.retry(resource, in, out, err);
				if (!retry) {
					return exitStatus;
				}
			}
		} finally {
			commandWrapper.after(resource, in, out, err);
		}
	}

	@Override
	public String toString() {
		return String.format("DefaultCommandWrapper[nested=%s,commandWrapper=%s]", nested, commandWrapper);
	}
}