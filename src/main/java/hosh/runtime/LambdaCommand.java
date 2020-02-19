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
package hosh.runtime;

import hosh.doc.Todo;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Key;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LambdaCommand implements Command, InterpreterAware, StateAware {

	private final Compiler.Statement statement;
	private final String key;
	private Interpreter interpreter;
	private State state;

	public LambdaCommand(Compiler.Statement statement, String key) {
		this.statement = statement;
		this.key = key;
	}

	@Override
	public void setInterpreter(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public void setState(State state) {
		this.state = state;
	}

	@Todo(description = "generalize with a list of keys")
	@Override
	public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
		Key internedKey = Keys.of(key);
		for (Record record : InputChannel.iterate(in)) {
			Value value = record.value(internedKey).orElseThrow(IllegalArgumentException::new);
			Map<String, String> original = state.getVariables();
			Map<String, String> modified = new HashMap<>(original);
			modified.put(key, value.unwrap(String.class).orElse("unwrap failed"));
			state.setVariables(modified);
			try {
				ExitStatus eval = interpreter.eval(statement, in, out, err);
				if (eval.isError()) {
					return eval;
				}
			} finally {
				// any change made by inner statement to variable will be lost here
				// create a new Resolvable implementation?
				state.setVariables(original);
			}
		}
		return ExitStatus.success();
	}

	public String getKey() {
		return key;
	}
}
