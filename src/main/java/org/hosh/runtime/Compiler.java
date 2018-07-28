package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hosh.antlr4.HoshParser;
import org.hosh.antlr4.HoshParser.CommandContext;
import org.hosh.antlr4.HoshParser.StmtContext;
import org.hosh.antlr4.HoshParser.WrapperContext;
import org.hosh.doc.Todo;
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
		if (stmt.command() != null) {
			return compileAsCommand(stmt.command());
		}
		if (stmt.wrapper() != null) {
			return compileAsWrappedCommand(stmt.wrapper());
		}
		throw new IllegalStateException("internal bug");
	}

	private Statement compileAsCommand(CommandContext ctx) {
		Token token = ctx.ID().get(0).getSymbol();
		String commandName = token.getText();
		Command command = commandResolver.tryResolve(commandName);
		if (command == null) {
			throw new CompileError("line " + token.getLine() + ": unknown command " + commandName);
		}
		List<String> commandArgs = compileArguments(ctx);
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
		return statement;
	}

	@Todo(description = "allows to grouping arguments by using ' or \" in the grammar (strings)")
	private List<String> compileArguments(CommandContext ctx) {
		return ctx
				.ID()
				.stream()
				.skip(1)
				.map(TerminalNode::getSymbol)
				.map(this::resolveVariable)
				.collect(Collectors.toList());
	}

	private Statement compileAsWrappedCommand(WrapperContext ctx) {
		Token token = ctx.ID().get(0).getSymbol();
		String commandName = token.getText();
		Command command = commandResolver.tryResolve(commandName);
		if (command == null) {
			throw new CompileError("line " + token.getLine() + ": unknown command wrapper " + commandName);
		}
		if (command instanceof CommandWrapper == false) {
			throw new CompileError("line " + token.getLine() + ": not a command wrapper " + commandName);
		}
		List<String> commandArgs = compileWrappedArguments(ctx);
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
		return statement;
	}

	@Todo(description = "allows to grouping arguments by using ' or \" in the grammar (strings)")
	private List<String> compileWrappedArguments(WrapperContext ctx) {
		return ctx
				.ID()
				.stream()
				.skip(1)
				.map(TerminalNode::getSymbol)
				.map(this::resolveVariable)
				.collect(Collectors.toList());
	}

	// resolves ${NAME} by looking for NAME in variables
	@Todo(description = "move this logic in a grammar production")
	private String resolveVariable(Token token) {
		String id = token.getText();
		if (id.startsWith("${")) {
			String variableName = id.substring(2, id.length() - 1);
			if (state.getVariables().containsKey(variableName)) {
				return state.getVariables().get(variableName);
			} else {
				throw new CompileError("line " + token.getLine() + ": unknown variable " + variableName);
			}
		} else {
			return id;
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
}
