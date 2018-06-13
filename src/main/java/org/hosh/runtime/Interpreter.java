package org.hosh.runtime;

import java.util.List;

import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;

public class Interpreter {

	private final Channel out;
	private final Channel err;

	public Interpreter(Channel out, Channel err) {
		this.out = out;
		this.err = err;
	}

	public void eval(Program program) {
		for (Statement statement : program.getStatements()) {
			Command command = statement.getCommand();
			List<String> arguments = statement.getArguments();
			command.run(arguments, out, err);
		}
	}
}
