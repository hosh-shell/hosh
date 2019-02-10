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
import java.util.concurrent.CancellationException;
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
 * Manages the runtime execution of both java/built-in commands as well as
 * external commands.
 */
public class Supervisor implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final List<Future<ExitStatus>> futures = Collections.synchronizedList(new LinkedList<>());
	private final Terminal terminal;

	public Supervisor(Terminal terminal) {
		this.terminal = terminal;
	}

	public void submit(Callable<ExitStatus> task) {
		Future<ExitStatus> future = executor.submit(task);
		LOGGER.finer(() -> String.format("adding future %s", future));
		futures.add(future);
	}

	public ExitStatus waitForAll(Channel err) {
		terminal.handle(Signal.INT, signal -> {
			futures.forEach(this::cancelIfStillRunning);
		});
		try {
			List<ExitStatus> results = new ArrayList<>();
			for (Future<ExitStatus> future : futures) {
				results.add(future.get());
			}
			ExitStatus firstErrorOrSuccess = results.stream().filter(es -> !es.isSuccess()).findFirst().orElse(ExitStatus.success());
			return firstErrorOrSuccess;
		} catch (CancellationException e) {
			LOGGER.log(Level.FINE, "got cancellation", e);
			return ExitStatus.error();
		} catch (InterruptedException e) {
			LOGGER.log(Level.FINE, "got interrupt", e);
			Thread.currentThread().interrupt();
			return ExitStatus.error();
		} catch (ExecutionException e) {
			LOGGER.log(Level.SEVERE, "caught exception", e);
			String details;
			if (e.getCause() != null && e.getCause().getMessage() != null) {
				details = e.getCause().getMessage();
			} else {
				details = "";
			}
			err.send(Record.of("error", Values.ofText(details)));
			return ExitStatus.error();
		} finally {
			terminal.handle(Signal.INT, SignalHandler.SIG_DFL);
		}
	}

	@Override
	public void close() {
		executor.shutdownNow();
	}

	@Todo(description = "ideally this method should be private")
	public void setThreadName(Statement statement) {
		Thread.currentThread().setName(String.format("command='%s %s'",
				statement.getCommand().getClass().getSimpleName(),
				String.join(" ", statement.getArguments())));
	}

	private void cancelIfStillRunning(Future<ExitStatus> future) {
		if (!future.isCancelled() && !future.isDone()) {
			LOGGER.finer(() -> String.format("cancelling future %s", future));
			future.cancel(true);
		}
	}
}
