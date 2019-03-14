/**
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
package org.hosh.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hosh.runtime.Compiler.Statement;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Record;
import org.hosh.spi.Values;

/**
 * Manages runtime execution of built-in commands as well as external commands.
 *
 * SIGINT is handled as well, if requested.
 */
public class Supervisor implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final List<Future<ExitStatus>> futures = new ArrayList<>();
	private boolean handleSignals = true;

	public void setHandleSignals(boolean handleSignals) {
		this.handleSignals = handleSignals;
	}

	@Override
	public void close() {
		executor.shutdownNow();
	}

	public void submit(Statement statement, Callable<ExitStatus> task) {
		Future<ExitStatus> future = executor.submit(() -> {
			setThreadName(statement);
			return task.call();
		});
		LOGGER.finer(() -> String.format("adding future %s", future));
		futures.add(future);
	}

	public ExitStatus waitForAll(Channel err) {
		cancelFuturesOnSigint();
		try {
			List<ExitStatus> results = waitForCompletion();
			return deriveExitStatus(results);
		} catch (CancellationException e) {
			LOGGER.log(Level.INFO, "got cancellation", e);
			return ExitStatus.error();
		} catch (InterruptedException e) {
			LOGGER.log(Level.INFO, "got interrupt", e);
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "caught exception", e);
			String message = messageFor(e);
			err.send(Record.of(Keys.ERROR, Values.ofText(message)));
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

	private ExitStatus deriveExitStatus(List<ExitStatus> results) {
		return results
				.stream()
				.filter(ExitStatus::isError)
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
		if (handleSignals) {
			LOGGER.fine("restoring default INT signal handler");
			org.jline.utils.Signals.registerDefault("INT");
		}
	}

	private void cancelFuturesOnSigint() {
		if (handleSignals) {
			LOGGER.fine("register INT signal handler");
			org.jline.utils.Signals.register("INT", () -> {
				LOGGER.info("  got INT signal");
				futures.forEach(this::cancelIfStillRunning);
			});
		}
	}

	private void cancelIfStillRunning(Future<ExitStatus> future) {
		if (future.isDone()) {
			return;
		}
		if (future.isCancelled()) {
			return;
		}
		LOGGER.finer(() -> String.format("cancelling future %s", future));
		future.cancel(true);
	}

	private void setThreadName(Statement statement) {
		String commandName = statement.getCommand().getClass().getSimpleName();
		List<String> commandWithArguments = new ArrayList<>();
		commandWithArguments.add(commandName);
		commandWithArguments.addAll(statement.getArguments());
		String name = String.format("command='%s'", String.join(" ", commandWithArguments));
		Thread.currentThread().setName(name);
	}
}
