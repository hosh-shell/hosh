/**
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
package org.hosh.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.hosh.antlr4.HoshParser;
import org.hosh.antlr4.HoshParser.ArgContext;
import org.hosh.antlr4.HoshParser.CommandContext;
import org.hosh.antlr4.HoshParser.InvocationContext;
import org.hosh.antlr4.HoshParser.PipelineContext;
import org.hosh.antlr4.HoshParser.SimpleContext;
import org.hosh.antlr4.HoshParser.StmtContext;
import org.hosh.antlr4.HoshParser.WrappedContext;
import org.hosh.spi.Command;
import org.hosh.spi.CommandWrapper;
import org.hosh.spi.State;

public class Compiler {

	private final CommandResolver commandResolver;

	public Compiler(CommandResolver commandResolver) {
		this.commandResolver = commandResolver;
	}

	public Program compile(String input) {
		Parser parser = new Parser();
		HoshParser.ProgramContext programContext = parser.parse(input);
		Program program = new Program();
		List<Statement> statements = new ArrayList<>();
		for (StmtContext stmtContext : programContext.stmt()) {
			Statement statement = compileStatement(stmtContext);
			statements.add(statement);
		}
		program.setStatements(statements);
		return program;
	}

	private Statement compileStatement(StmtContext ctx) {
		return compilePipeline(ctx.pipeline());
	}

	private Statement compilePipeline(PipelineContext ctx) {
		if (ctx.getChildCount() == 1) { // simple command
			return compileCommand(ctx.command());
		}
		if (ctx.getChildCount() == 2) { // unfinished pipeline such as "command | "
			throw new CompileError(String.format("line %d:%d: incomplete pipeline near '%s'", ctx.getStart().getLine(),
					ctx.getStop().getCharPositionInLine(), ctx.getStop().getText()));
		}
		if (ctx.getChildCount() == 3) { // pipeline
			Statement producer = compileCommand(ctx.command());
			Statement consumer = compileStatement(ctx.stmt());
			Statement pipeline = new Statement();
			pipeline.setCommand(new PipelineCommand(producer, consumer));
			pipeline.setArguments(Collections.emptyList());
			pipeline.setLocation("");
			return pipeline;
		}
		throw new InternalBug(ctx);
	}

	private Statement compileCommand(CommandContext ctx) {
		if (ctx.simple() != null) {
			return compileSimple(ctx.simple());
		}
		if (ctx.wrapped() != null) {
			return compileWrappedCommand(ctx.wrapped());
		}
		throw new InternalBug(ctx);
	}

	private Statement compileSimple(SimpleContext ctx) {
		Token token = ctx.invocation().ID().getSymbol();
		String commandName = token.getText();
		Optional<Command> resolvedCommand = commandResolver.tryResolve(commandName);
		Command command = resolvedCommand
				.orElseThrow(() -> new CompileError(String.format("line %d: '%s' unknown command", token.getLine(), commandName)));
		command.downCast(CommandWrapper.class).ifPresent(cmd -> {
			throw new CompileError(String.format("line %d: '%s' is a command wrapper", token.getLine(), commandName));
		});
		List<Resolvable> commandArgs = compileArguments(ctx.invocation());
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
		statement.setLocation(commandName);
		return statement;
	}

	private Statement compileWrappedCommand(WrappedContext ctx) {
		if (ctx.wrapped() != null) {
			int line = ctx.start.getLine();
			throw new CompileError(String.format("line %d: unnecessary closing '}'", line));
		}
		Token token = ctx.invocation().ID().getSymbol();
		String commandName = token.getText();
		Optional<Command> resolvedCommand = commandResolver.tryResolve(commandName);
		Command command = resolvedCommand
				.orElseThrow(() -> new CompileError(String.format("line %d: '%s' unknown command wrapper", token.getLine(), commandName)));
		CommandWrapper<?> commandWrapper = command.downCast(CommandWrapper.class)
				.orElseThrow(() -> new CompileError(String.format("line %d: '%s' is not a command wrapper", token.getLine(), commandName)));
		if (ctx.stmt() == null) {
			int line = ctx.start.getLine();
			throw new CompileError(String.format("line %d: '%s' with empty wrapping statement", line, commandName));
		}
		Statement nestedStatement = compileStatement(ctx.stmt());
		List<Resolvable> commandArgs = compileArguments(ctx.invocation());
		Statement statement = new Statement();
		statement.setCommand(new DefaultCommandWrapper<>(nestedStatement, commandWrapper));
		statement.setArguments(commandArgs);
		statement.setLocation(commandName);
		return statement;
	}

	private List<Resolvable> compileArguments(InvocationContext ctx) {
		return ctx
				.arg()
				.stream()
				.map(this::compileArgument)
				.collect(Collectors.toList());
	}

	private Resolvable compileArgument(ArgContext ctx) {
		if (ctx.ID() != null) {
			Token token = ctx.ID().getSymbol();
			String value = token.getText();
			return new Constant(value);
		}
		if (ctx.VARIABLE() != null) {
			Token token = ctx.VARIABLE().getSymbol();
			String name = dropDeref(token.getText());
			return new Variable(name);
		}
		if (ctx.VARIABLE_OR_FALLBACK() != null) {
			Token token = ctx.VARIABLE_OR_FALLBACK().getSymbol();
			String[] nameAndFallback = dropDeref(token.getText()).split("!");
			return new VariableOrFallback(nameAndFallback[0], nameAndFallback[1]);
		}
		if (ctx.STRING() != null) {
			Token token = ctx.STRING().getSymbol();
			String value = dropQuotes(token);
			return new Constant(value);
		}
		throw new InternalBug(ctx);
	}

	// "some text" -> some text
	private String dropQuotes(Token token) {
		String text = token.getText();
		return text.substring(1, text.length() - 1);
	}

	// ${VARIABLE} -> VARIABLE
	private String dropDeref(String variable) {
		return variable.substring(2, variable.length() - 1);
	}

	public static class Program {

		private List<Statement> statements;

		public void setStatements(List<Statement> statements) {
			this.statements = statements;
		}

		public List<Statement> getStatements() {
			return statements;
		}
	}

	public static class Statement {

		private Command command;

		private List<Resolvable> arguments;

		private String location;

		public void setCommand(Command command) {
			this.command = command;
		}

		public Command getCommand() {
			return command;
		}

		public List<Resolvable> getArguments() {
			return arguments;
		}

		public void setArguments(List<Resolvable> arguments) {
			this.arguments = arguments;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}
	}

	interface Resolvable {

		// resolve or throw
		String resolve(State state);
	}

	public static class Constant implements Resolvable {

		private final String value;

		public Constant(String value) {
			this.value = value;
		}

		@Override
		public String resolve(State state) {
			return value;
		}
	}

	public static class Variable implements Resolvable {

		private final String name;

		public Variable(String name) {
			this.name = name;
		}

		@Override
		public String resolve(State state) {
			String value = state.getVariables().get(name);
			if (value == null) {
				throw new IllegalStateException(String.format("cannot resolve variable: %s", name));
			}
			return value;
		}
	}

	public static class VariableOrFallback implements Resolvable {

		private final String name;

		private final String fallback;

		public VariableOrFallback(String name, String fallback) {
			this.name = name;
			this.fallback = fallback;
		}

		@Override
		public String resolve(State state) {
			return state.getVariables().getOrDefault(name, fallback);
		}
	}

	public static class CompileError extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public CompileError(String message) {
			super(message);
		}
	}

	public static class InternalBug extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public InternalBug(ParseTree parseTree) {
			super("internal bug in compiler near: " + parseTree.getText());
		}
	}
}
