package org.hosh.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.hosh.doc.Todo;
import org.hosh.spi.Ansi;
import org.hosh.spi.State;

public class Prompt {

	private final State state;

	public Prompt(State state) {
		this.state = state;
	}

	@Todo(description = "this should be user-configurable")
	public String compute() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		Ansi.Style.FG_BLUE.enable(pw);
		pw.append("hosh:");
		Ansi.Style.FG_CYAN.enable(pw);
		pw.append(String.valueOf(state.getId()));
		Ansi.Style.FG_BLUE.enable(pw);
		pw.append("> ");
		Ansi.Style.RESET.enable(pw);
		return sw.toString();
	}
}
