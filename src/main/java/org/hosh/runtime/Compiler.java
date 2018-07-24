package org.hosh.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hosh.antlr4.HoshParser;
import org.hosh.antlr4.HoshParser.StmtContext;
import org.hosh.doc.Todo;
import org.hosh.spi.Command;
import org.hosh.spi.State;

public class Compiler {

	private final State state;

	public Compiler(State state) {
		this.state = state;
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
		Token token = stmt.command().ID().get(0).getSymbol();
		String commandName = token.getText();
		Command command = state.getCommands().get(commandName);
		if (command == null) {
			command = resolveCommandInPath(commandName, state.getPath());
		}
		if (command == null) {
			throw new CompileError("line " + token.getLine() + ": unknown command " + commandName);
		}
		List<String> commandArgs = compileArguments(stmt);
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
		return statement;
	}


	private Command resolveCommandInPath(String commandName, List<Path> path) {
		Path candidate = Paths.get(commandName);
		if (candidate.isAbsolute() && Files.exists(candidate)) {
			return new ExternalCommand(candidate);
		}
		for (Path dir : path) {
			candidate = Paths.get(dir.toString(), commandName);
			boolean exists = Files.exists(candidate);
			if (exists) {
				return new ExternalCommand(candidate);
			}
		}
		return null;
	}

	@Todo(description="allows to grouping arguments by using ' or \"")
	private List<String> compileArguments(StmtContext stmt) {
		return stmt
				.command()
				.ID()
				.stream()
				.skip(1)
				.map(TerminalNode::getSymbol)
				.map(this::resolveVariable)
				.collect(Collectors.toList());
	}

	// resolves ${NAME} by looking for NAME in variables
	@Todo(description="move this logic in a grammar production")
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
