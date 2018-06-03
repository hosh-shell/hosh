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
	private final CommandFactory commandFactory;

	public Compiler(State state, CommandFactory commandFactory) {
		this.state = state;
		this.commandFactory = commandFactory;
	}

	public Program compile(String line) {
		HoshParser.ProgramContext programContext = Parser.parse(line + '\n');
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
		String commandName = stmt.ID().get(0).getSymbol().getText();
		Class<? extends Command> search = state.getCommands().get(commandName);
		Command command = commandFactory.create(search);
		if (search == null) {
			throw new CompileError("command not found");
		}
		List<String> commandArgs = compileArguments(stmt);
		Statement statement = new Statement();
		statement.setCommand(command);
		statement.setArguments(commandArgs);
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

	}

	public static class CompileError extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public CompileError(String message) {
			super(message);
		}

	}
}
