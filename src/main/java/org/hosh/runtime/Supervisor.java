/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hosh.doc.Todo;
import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Record;
import org.hosh.spi.Values;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;

/**
 * Manages runtime execution of built-in commands as well as external commands.
 *
 * SIGINT is handled as well.
 */
public class Supervisor implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final List<Future<ExitStatus>> futures = Collections.synchronizedList(new LinkedList<>());
	private final Terminal terminal;

	public Supervisor(Terminal terminal) {
		this.terminal = terminal;
	}

	@Override
	public void close() {
		executor.shutdownNow();
	}

	@Todo(description = "ideally this method should be called by submit() and never exposed outside")
	public void setThreadName(Statement statement) {
		String commandName = statement.getCommand().getClass().getSimpleName();
		String arguments = String.join(" ", statement.getArguments());
		String name = String.format("command='%s%s%s'", commandName, arguments.isEmpty() ? "" : " ", arguments);
		Thread.currentThread().setName(name);
	}

	public void submit(Callable<ExitStatus> task) {
		Future<ExitStatus> future = executor.submit(task);
		LOGGER.finer(() -> String.format("adding future %s", future));
		futures.add(future);
	}

	public ExitStatus waitForAll(Channel err) {
		cancelFuturesOnSigint();
		try {
			List<ExitStatus> results = waitForCompletion();
			return exitStatusFrom(results);
		} catch (InterruptedException e) {
			LOGGER.log(Level.FINE, "got interrupt", e);
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "caught exception", e);
			String message = messageFor(e);
			err.send(Record.of("error", Values.ofText(message)));
			return ExitStatus.error();
		} finally {
			restoreDefaultSigintHandler();
		}
	}

	private String messageFor(ExecutionException e) {
		if (e.getCause() != null && e.getCause().getMessage() != null) {
			return e.getCause().getMessage();
		} else {
			return "(no message provided)";
		}
	}

	private ExitStatus exitStatusFrom(List<ExitStatus> results) {
		return results.stream()
				.filter(es -> !es.isSuccess())
				.findFirst()
				.orElse(ExitStatus.success());
	}

	private List<ExitStatus> waitForCompletion() throws InterruptedException, ExecutionException {
		List<ExitStatus> results = new ArrayList<>();
		for (Future<ExitStatus> future : futures) {
			results.add(future.get());
		}
		return results;
	}

	private void restoreDefaultSigintHandler() {
		terminal.handle(Signal.INT, SignalHandler.SIG_DFL);
	}

	private void cancelFuturesOnSigint() {
		terminal.handle(Signal.INT, signal -> futures.forEach(this::cancelIfStillRunning));
	}

	private void cancelIfStillRunning(Future<ExitStatus> future) {
		if (!future.isCancelled() && !future.isDone()) {
			LOGGER.finer(() -> String.format("cancelling future %s", future));
			future.cancel(true);
		}
	}
}
