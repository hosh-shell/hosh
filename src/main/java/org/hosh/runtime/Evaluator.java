package org.hosh.runtime;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.hosh.antlr4.HoshParser;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.State;

public class Evaluator {

	private final State state;
	private final CommandFactory commandFactory;

	public Evaluator(State state, CommandFactory commandFactory) {
		this.state = state;
		this.commandFactory = commandFactory;
	}

	public void run(String line, Channel out, Channel err) {
		HoshParser.ProgramContext programContext = Parser.parse(line + '\n');
		programContext.stmt().forEach(stmt -> {
			String commandName = stmt.ID().get(0).getSymbol().getText();
			Class<? extends Command> search = state.getCommands().get(commandName);
			Command command = commandFactory.create(search);
			if (search != null) {
				List<String> commandArgs = stmt.ID().stream().skip(1).map(TerminalNode::getSymbol).map(Token::getText).collect(Collectors.toList());
				command.run(commandArgs, out, err);
			} else {
				throw new IllegalArgumentException("command not found");
			}
		});
	}
}
