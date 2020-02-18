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

import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Key;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Value;

import java.util.List;
import java.util.Map;

public class LambdaCommand implements Command, InterpreterAware, StateAware {

	private final Key key;
	private final Compiler.Statement statement;
	private Interpreter interpreter;
	private State state;

	public LambdaCommand(Key key, Compiler.Statement statement) {
		this.key = key;
		this.statement = statement;
	}

	@Override
	public void setInterpreter(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public void setState(State state) {
		this.state = state;
	}

	@Override
	public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
		for (Record record : InputChannel.iterate(in)) {
			Value value = record.value(key).orElseThrow(IllegalArgumentException::new);
			Map<String, String> variables = state.getVariables();
			variables.put(key.name(), value.unwrap(String.class).orElse("unwrap failed"));
			ExitStatus eval = interpreter.eval(statement, in, out, err);
			if (eval.isError()) {
				return eval;
			}
		}
		state.getVariables().remove(key.name());
		return ExitStatus.success();
	}
}
