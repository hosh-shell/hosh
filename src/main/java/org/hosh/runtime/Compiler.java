package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.hosh.antlr4.HoshParser;
import org.hosh.antlr4.HoshParser.ArgContext;
import org.hosh.antlr4.HoshParser.CommandContext;
import org.hosh.antlr4.HoshParser.InvocationContext;
import org.hosh.antlr4.HoshParser.SimpleContext;
import org.hosh.antlr4.HoshParser.StmtContext;
import org.hosh.antlr4.HoshParser.WrapperContext;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandWrapper;
import org.hosh.spi.State;

public class Compiler {
	private final State state;
	private final CommandResolver commandResolver;

	public Compiler(State state, CommandResolver commandResolver) {
		this.state = state;
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

	private Statement compileStatement(StmtContext stmt) {
		CommandContext command = stmt.command();
		if (command.simple() != null) {
			return compileSimpleCommand(command.simple());
		}
		if (command.wrapper() != null) {
			return compileWrappedCommand(command.wrapper());
		}
		throw new InternalBug();
	}

	private Statement compileSimpleCommand(SimpleContext simple) {
		Token token = simple.invocation().ID().getSymbol();
		String commandName = token.getText();
		Command command = commandResolver.tryResolve(commandName);
		if (command == null) {
			throw new CompileError("line " + token.getLine() + ": unknown command " + commandName);
		}
		List<String> commandArgs = compileArguments(simple.invocation());
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
		return statement;
	}

	private Statement compileWrappedCommand(WrapperContext ctx) {
		Token token = ctx.invocation().ID().getSymbol();
		String commandName = token.getText();
		Command command = commandResolver.tryResolve(commandName);
		if (command == null) {
			throw new CompileError("line " + token.getLine() + ": unknown command wrapper " + commandName);
		}
		if (command instanceof CommandWrapper == false) {
			throw new CompileError("line " + token.getLine() + ": not a command wrapper " + commandName);
		}
		@SuppressWarnings("unchecked")
		CommandWrapper<Object> commandWrapper = (CommandWrapper<Object>) command;
		Statement nestedStatement = compileSimpleCommand(ctx.simple());
		List<String> commandArgs = compileArguments(ctx.invocation());
		Statement statement = new Statement();
		statement.setCommand(new GeneratedCommand(nestedStatement, commandWrapper));
		statement.setArguments(commandArgs);
		return statement;
	}

	private List<String> compileArguments(InvocationContext ctx) {
		return ctx
				.arg()
				.stream()
				.map(this::compileArgument)
				.collect(Collectors.toList());
	}

	private String compileArgument(ArgContext ctx) {
		if (ctx.VARIABLE() != null) {
			Token token = ctx.VARIABLE().getSymbol();
			String variableName = variableNameFromToken(token);
			if (state.getVariables().containsKey(variableName)) {
				return state.getVariables().get(variableName);
			} else {
				throw new CompileError("line " + token.getLine() + ": unknown variable " + variableName);
			}
		}
		if (ctx.ID() != null) {
			Token token = ctx.ID().getSymbol();
			return token.getText();
		}
		if (ctx.STRING() != null) {
			Token token = ctx.STRING().getSymbol();
			return dropQuotes(token);
		}
		throw new InternalBug();
	}

	// "some text" -> some text
	private String dropQuotes(Token token) {
		String text = token.getText();
		return text.substring(1, text.length() - 1);
	}

	// ${VARIABLE} -> VARIABLE
	private String variableNameFromToken(Token token) {
		return token.getText().substring(2, token.getText().length() - 1);
	}

	public static final class GeneratedCommand implements Command {
		private final Statement nestedStatement;
		private final CommandWrapper<Object> commandWrapper;

		private GeneratedCommand(Statement nestedStatement, CommandWrapper<Object> commandWrapper) {
			this.nestedStatement = nestedStatement;
			this.commandWrapper = commandWrapper;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			Object resource = commandWrapper.before(args, out, err);
			try {
				nestedStatement.command.run(nestedStatement.arguments, out, err);
			} finally {
				commandWrapper.after(resource, out, err);
			}
		}

		public Statement getNestedStatement() {
			return nestedStatement;
		}

		@Override
		public String toString() {
			return String.format("GeneratedCommandWrapper[nested=%s,wrapper=%s]", nestedStatement, commandWrapper);
		}
	}

	public static class Program {
		private List<Statement> statements;

		public void setStatements(List<Statement> statements) {
			this.statements = statements;
		}

		public List<Statement> getStatements() {
			return statements;
		}

		@Override
		public String toString() {
			return String.format("Program[%s]", statements);
		}
	}

	public static class Statement {
		private Command command;
		private List<String> arguments;

		public void setCommand(Command command) {
			this.command = command;
		}

		public Command getCommand() {
			return command;
		}

		public List<String> getArguments() {
			return arguments;
		}

		public void setArguments(List<String> arguments) {
			this.arguments = arguments;
		}

		@Override
		public String toString() {
			return String.format("Statement[class=%s,arguments=%s]", command.getClass().getCanonicalName(), arguments);
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
	}
}
