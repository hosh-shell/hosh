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
import hosh.spi.CommandWrapper;
import hosh.spi.State;
import hosh.spi.VariableName;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static hosh.runtime.antlr4.HoshParser.CommandContext;
import static hosh.runtime.antlr4.HoshParser.DqstringContext;
import static hosh.runtime.antlr4.HoshParser.ExpansionContext;
import static hosh.runtime.antlr4.HoshParser.ExpressionContext;
import static hosh.runtime.antlr4.HoshParser.InvocationContext;
import static hosh.runtime.antlr4.HoshParser.LambdaContext;
import static hosh.runtime.antlr4.HoshParser.PipelineContext;
import static hosh.runtime.antlr4.HoshParser.ProgramContext;
import static hosh.runtime.antlr4.HoshParser.SequenceContext;
import static hosh.runtime.antlr4.HoshParser.SimpleContext;
import static hosh.runtime.antlr4.HoshParser.SqstringContext;
import static hosh.runtime.antlr4.HoshParser.StmtContext;
import static hosh.runtime.antlr4.HoshParser.StringContext;
import static hosh.runtime.antlr4.HoshParser.WrappedContext;

/**
 * Translates the incoming Hosh string into a runnable {@link Program}.
 */
public class Compiler {

	private final CommandResolver commandResolver;

	public Compiler(CommandResolver commandResolver) {
		this.commandResolver = commandResolver;
	}

	public Program compile(String input) {
		Parser parser = new Parser();
		ProgramContext programContext = parser.parse(input);
		List<Statement> statements = new ArrayList<>();
		for (StmtContext ctx : programContext.stmt()) {
			Statement statement = compileStatement(ctx);
			statements.add(statement);
		}
		return new Program(statements);
	}

	private Statement compileStatement(StmtContext ctx) {
		if (ctx.sequence() != null) {
			return compileSequence(ctx.sequence());
		}
		throw new InternalBug(ctx);
	}

	private Statement compileSequence(SequenceContext ctx) {
		if (ctx.getChildCount() == 1) {
			return compilePipeline(ctx.pipeline());
		}
		if (ctx.getChildCount() == 3) {
			Statement first = compilePipeline(ctx.pipeline());
			Statement second = compileSequence(ctx.sequence());
			SequenceCommand command = new SequenceCommand(first, second);
			return new Statement(command, List.of(), "");
		}
		throw new InternalBug(ctx);
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
			PipelineCommand command = new PipelineCommand(producer, consumer);
			return new Statement(command, List.of(), "");
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
		if (ctx.lambda() != null) {
			return compileLambda(ctx.lambda());
		}
		throw new InternalBug(ctx);
	}

	private Statement compileSimple(SimpleContext ctx) {
		Token token = ctx.invocation().ID().getSymbol();
		String commandName = token.getText();
		Optional<Command> resolvedCommand = commandResolver.tryResolve(commandName);
		Command command = resolvedCommand
				.orElseThrow(() -> new CompileError(String.format("line %d: '%s' unknown command", token.getLine(), commandName)));
		if (command instanceof CommandWrapper) {
			throw new CompileError(String.format("line %d: '%s' is a command wrapper", token.getLine(), commandName));
		}
		List<Resolvable> arguments = compileArguments(ctx.invocation());
		return new Statement(command, arguments, commandName);
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

		if (ctx.stmt() == null) {
			int line = ctx.start.getLine();
			throw new CompileError(String.format("line %d: '%s' with empty wrapping statement", line, commandName));
		}
		Statement nestedStatement = compileStatement(ctx.stmt());
		List<Resolvable> arguments = compileArguments(ctx.invocation());
		if (command instanceof CommandWrapper commandWrapper) {
			DefaultCommandDecorator decoratedCommand = new DefaultCommandDecorator(nestedStatement, commandWrapper);
			return new Statement(decoratedCommand, arguments, commandName);
		}
		throw new CompileError(String.format("line %d: '%s' is not a command wrapper", token.getLine(), commandName));
	}

	private Statement compileLambda(LambdaContext ctx) {
		Statement nestedStatement = compileSimple(ctx.simple());
		String key = ctx.ID().getSymbol().getText();
		return new Statement(new LambdaCommand(nestedStatement, key), List.of(), "");
	}

	private List<Resolvable> compileArguments(InvocationContext ctx) {
		return ctx
				.expression()
				.stream()
				.map(this::compileArgument)
				.toList();
	}

	private Resolvable compileArgument(ExpressionContext ctx) {
		if (ctx.ID() != null) {
			Token token = ctx.ID().getSymbol();
			String value = token.getText();
			return new Constant(value);
		}
		if (ctx.expansion() != null) {
			return compileExpansion(ctx.expansion());
		}
		if (ctx.string() != null) {
			return compileString(ctx.string());
		}
		throw new InternalBug(ctx);
	}

	private Resolvable compileString(StringContext ctx) {
		if (ctx.sqstring() != null) {
			return compileSingleQuotedString(ctx.sqstring());
		}
		if (ctx.dqstring() != null) {
			return compileDoubleQuotedString(ctx.dqstring());
		}
		throw new InternalBug(ctx);
	}

	private Resolvable compileDoubleQuotedString(DqstringContext ctx) {
		List<Resolvable> result = new ArrayList<>();
		for (var part : ctx.dqpart()) {
			if (part.DQUOTE_TEXT() != null) {
				result.add(new Constant(part.DQUOTE_TEXT().getSymbol().getText()));
			} else if (part.DQUOTE_VARIABLE() != null) {
				Token token = part.DQUOTE_VARIABLE().getSymbol();
				String name = dropDeref(token.getText());
				result.add(new Variable(VariableName.constant(name)));
			} else if (part.DQUOTE_VARIABLE_OR_FALLBACK() != null) {
				Token token = part.DQUOTE_VARIABLE_OR_FALLBACK().getSymbol();
				String[] nameAndFallback = dropDeref(token.getText()).split("!", 2);
				result.add(new VariableOrFallback(VariableName.constant(nameAndFallback[0]), nameAndFallback[1]));
			} else {
				throw new InternalBug(ctx);
			}
		}
		return new Composite(result);
	}

	private Resolvable compileSingleQuotedString(SqstringContext ctx) {
		List<Resolvable> result = new ArrayList<>();
		for (var text : ctx.SQUOTE_TEXT()) {
			result.add(new Constant(text.getText()));
		}
		return new Composite(result);
	}

	private Resolvable compileExpansion(ExpansionContext ctx) {
		if (ctx.VARIABLE() != null) {
			Token token = ctx.VARIABLE().getSymbol();
			String name = dropDeref(token.getText());
			return new Variable(VariableName.constant(name));
		}
		if (ctx.VARIABLE_OR_FALLBACK() != null) {
			Token token = ctx.VARIABLE_OR_FALLBACK().getSymbol();
			String[] nameAndFallback = dropDeref(token.getText()).split("!", 2);
			return new VariableOrFallback(VariableName.constant(nameAndFallback[0]), nameAndFallback[1]);
		}
		throw new InternalBug(ctx);
	}

	// ${VARIABLE} -> VARIABLE
	private String dropDeref(String variable) {
		return variable.substring(2, variable.length() - 1);
	}

	public static class Program {

		private final List<Statement> statements;

		public Program(List<Statement> statements) {
			this.statements = statements;
		}

		public List<Statement> getStatements() {
			return statements;
		}
	}

	public static class Statement {

		private final Command command;

		private final List<Resolvable> arguments;

		private final String location;

		public Statement(Command command, List<Resolvable> arguments, String location) {
			this.command = command;
			this.arguments = arguments;
			this.location = location;
		}

		public Command getCommand() {
			return command;
		}

		public List<Resolvable> getArguments() {
			return arguments;
		}

		/**
		 * Describe command in a human-readable form: the main purpose is to automatically adding the location of an error.
		 * <p>
		 * For example, assuming the following pipeline "ls | cmd1 | cmd2",
		 * if cmd1 is returning error, the error message will start with "cmd1".
		 */
		public String getLocation() {
			return location;
		}

	}

	public interface Resolvable {

		// resolve or throw
		String resolve(State state);
	}

	public static class Composite implements Resolvable {

		private final List<Resolvable> resolvables;

		public Composite(List<Resolvable> resolvables) {
			this.resolvables = resolvables;
		}

		@Override
		public String resolve(State state) {
			StringBuilder result = new StringBuilder();
			for (Resolvable resolvable : resolvables) {
				result.append(resolvable.resolve(state));
			}
			return result.toString();
		}
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

		private final VariableName name;

		public Variable(VariableName name) {
			this.name = name;
		}

		@Override
		public String resolve(State state) {
			String value = state.getVariables().get(name);
			if (value == null) {
				throw new IllegalStateException(String.format("cannot resolve variable: %s", name.name()));
			}
			return value;
		}
	}

	public static class VariableOrFallback implements Resolvable {

		private final VariableName name;

		private final String fallback;

		public VariableOrFallback(VariableName name, String fallback) {
			this.name = name;
			this.fallback = fallback;
		}

		@Override
		public String resolve(State state) {
			return state.getVariables().getOrDefault(name, fallback);
		}
	}

	public static class CompileError extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 1L;

		public CompileError(String message) {
			super(message);
		}
	}

	public static class InternalBug extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 1L;

		public InternalBug(ParseTree parseTree) {
			super("internal bug in compiler near: " + parseTree.getText());
		}
	}

}
