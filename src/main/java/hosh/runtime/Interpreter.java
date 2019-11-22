/*
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
package hosh.runtime;

import hosh.doc.Todo;
import hosh.runtime.Compiler.Program;
import hosh.runtime.Compiler.Resolvable;
import hosh.runtime.Compiler.Statement;
import hosh.spi.OutputChannel;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.HistoryAware;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.LineReaderAware;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.TerminalAware;
import hosh.spi.Values;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Interpreter {

	private final State state;

	private final Terminal terminal;

	// this is a "private" LineReader to be injected in commands: it has no history
	// and no auto-complete
	private final LineReader lineReader;

	private final OutputChannel out;

	private final OutputChannel err;

	private History history = new NoHistory();

	public Interpreter(State state, Terminal terminal, OutputChannel out, OutputChannel err) {
		this.state = state;
		this.terminal = terminal;
		this.lineReader = LineReaderBuilder.builder().terminal(terminal).build();
		this.out = out;
		this.err = err;
	}

	public void setHistory(History history) {
		this.history = history;
	}

	public ExitStatus eval(Program program) {
		ExitStatus exitStatus = ExitStatus.success();
		for (Statement statement : program.getStatements()) {
			exitStatus = execute(statement);
			store(exitStatus);
			if (userRequestedExit() || lastCommandFailed(exitStatus)) {
				break;
			}
		}
		return exitStatus;
	}

	private boolean lastCommandFailed(ExitStatus exitStatus) {
		return exitStatus.isError();
	}

	private boolean userRequestedExit() {
		return state.isExit();
	}

	private void store(ExitStatus exitStatus) {
		Objects.requireNonNull(exitStatus, "exit status cannot be null");
		state.getVariables().put("EXIT_STATUS", String.valueOf(exitStatus.value()));
	}

	private ExitStatus execute(Statement statement) {
		try (Supervisor supervisor = new Supervisor()) {
			runSupervised(statement, supervisor);
			return supervisor.waitForAll();
		} catch (ExecutionException e) {
			Record record = Records.builder()
					.entry(Keys.LOCATION, Values.ofText(statement.getLocation()))
					.entry(Keys.ERROR, Values.ofText(messageFor(e)))
					.build();
			err.send(record);
			return ExitStatus.error();
		}
	}

	private void runSupervised(Statement statement, Supervisor supervisor) {
		supervisor.submit(() -> run(statement));
	}

	private ExitStatus run(Statement statement) {
		return run(statement, new NullChannel(), out, err);
	}

	protected ExitStatus run(Statement statement, InputChannel in, OutputChannel out, OutputChannel err) {
		Command command = statement.getCommand();
		injectDeps(command);
		List<String> resolvedArguments = resolveArguments(statement.getArguments());
		changeCurrentThreadName(statement.getLocation(), resolvedArguments);
		return command.run(resolvedArguments, in, out, new WithLocation(err, statement.getLocation()));
	}

	private void changeCurrentThreadName(String commandName, List<String> resolvedArguments) {
		List<String> commandWithArguments = new ArrayList<>();
		commandWithArguments.add(commandName);
		commandWithArguments.addAll(resolvedArguments);
		String name = String.format("command='%s'", String.join(" ", commandWithArguments));
		Thread.currentThread().setName(name);
	}

	@Todo(description = "extract and unit test in a custom class (e.g. Injector?)")
	protected void injectDeps(Command command) {
		Downcast.of(command, StateAware.class).ifPresent(cmd -> cmd.setState(state));
		Downcast.of(command, TerminalAware.class).ifPresent(cmd -> cmd.setTerminal(terminal));
		Downcast.of(command, LineReaderAware.class).ifPresent(cmd -> cmd.setLineReader(lineReader));
		Downcast.of(command, HistoryAware.class).ifPresent(cmd -> cmd.setHistory(history));
		Downcast.of(command, InterpreterAware.class).ifPresent(cmd -> cmd.setInterpreter(this));
	}

	private List<String> resolveArguments(List<Resolvable> arguments) {
		return arguments
				.stream()
				.map(resolvable -> resolvable.resolve(state))
				.collect(Collectors.toList());
	}

	private String messageFor(ExecutionException e) {
		if (e.getCause() != null && e.getCause().getMessage() != null) {
			return e.getCause().getMessage();
		} else {
			return "(no message provided)";
		}
	}

	// enrich any record sent to the inner channel
	// with location of the current statement
	private static class WithLocation implements OutputChannel {

		private final OutputChannel channel;

		private final String location;

		public WithLocation(OutputChannel channel, String location) {
			this.channel = channel;
			this.location = location;
		}

		@Override
		public void send(Record record) {
			channel.send(record.prepend(Keys.LOCATION, Values.ofText(location)));
		}

	}

	/** Fake implementation of history, used for non-interactive sessions (i.e. scripts) */
	private static class NoHistory implements History {

		@Override
		public void attach(LineReader reader) {
		}

		@Override
		public void load() {
		}

		@Override
		public void save() {
		}

		@Override
		public void write(Path file, boolean incremental) {
		}

		@Override
		public void append(Path file, boolean incremental) {
		}

		@Override
		public void read(Path file, boolean incremental) {
		}

		@Override
		public void purge() {
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public int index() {
			return 0;
		}

		@Override
		public int first() {
			return 0;
		}

		@Override
		public int last() {
			return 0;
		}

		@Override
		public String get(int index) {
			return null;
		}

		@Override
		public void add(Instant time, String line) {
		}

		@Override
		public ListIterator<Entry> iterator(int index) {
			return Collections.emptyListIterator();
		}

		@Override
		public String current() {
			return null;
		}

		@Override
		public boolean previous() {
			return false;
		}

		@Override
		public boolean next() {
			return false;
		}

		@Override
		public boolean moveToFirst() {
			return false;
		}

		@Override
		public boolean moveToLast() {
			return false;
		}

		@Override
		public boolean moveTo(int index) {
			return false;
		}

		@Override
		public void moveToEnd() {
		}
	}
}
