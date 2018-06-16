package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hosh.antlr4.HoshParser;
import org.hosh.antlr4.HoshParser.StmtContext;
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
		Token token = stmt.ID().get(0).getSymbol();
		String commandName = token.getText();
		Command command = state.getCommands().get(commandName);
		if (command == null) {
			throw new CompileError("line " + token.getLine() + ": unknown command " + commandName);
		}
		List<String> commandArgs = compileArguments(stmt);
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
		statement.setLineNumber(token.getLine());
		return statement;
	}

	private List<String> compileArguments(StmtContext stmt) {
		return stmt.ID().stream().skip(1).map(TerminalNode::getSymbol).map(Token::getText).collect(Collectors.toList());
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
		private List<String> arguments;
		private int lineNumber;

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

		public void setLineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
		}

		public int getLineNumber() {
			return lineNumber;
		}

	}

	public static class CompileError extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public CompileError(String message) {
			super(message);
		}

	}
}
