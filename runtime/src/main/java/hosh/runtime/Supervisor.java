/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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

import hosh.spi.ExitStatus;
import hosh.spi.LoggerFactory;
import org.jline.terminal.Terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages execution of built-in commands as well as external commands via {@link Supervisor#submit(Callable)}
 * while providing a synchronization point via {@link Supervisor#waitForAll()}.
 * <p>
 * SIGINT is handled via JLine's Terminal.handle() when a Terminal is provided.
 * This intercepts the signal before the JVM acts on it, allowing the shell to
 * cancel the current pipeline and return to the prompt rather than exiting.
 */
class Supervisor implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private final ExecutorService executor;
	private final List<Future<ExitStatus>> futures;
	private final Terminal terminal;
	private Terminal.SignalHandler previousSigintHandler;

	// No signal handling — used inside PipelineCommand where the outer Supervisor already handles signals.
	public Supervisor() {
		this(null);
	}

	// With JLine-based SIGINT interception — used in Interpreter for top-level command execution.
	public Supervisor(Terminal terminal) {
		this.terminal = terminal;
		this.futures = new CopyOnWriteArrayList<>();
		this.executor = Executors.newVirtualThreadPerTaskExecutor();
	}

	@Override
	public void close() {
		executor.shutdownNow();
	}

	public void submit(Callable<ExitStatus> task) {
		Future<ExitStatus> future = executor.submit(task);
		LOGGER.finer(() -> String.format("adding future %s", future));
		futures.add(future);
	}

	public ExitStatus waitForAll() throws ExecutionException {
		installSigintHandler();
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
		} finally {
			restoreDefaultSigintHandler();
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

	private void installSigintHandler() {
		if (terminal != null) {
			LOGGER.fine("register INT signal handler");
			previousSigintHandler = terminal.handle(Terminal.Signal.INT, signal -> {
				LOGGER.info("SIGINT received, cancelling futures...");
				for (Future<ExitStatus> future : futures) {
					LOGGER.finer(() -> String.format("cancelling future %s", future));
					future.cancel(true);
				}
			});
		}
	}

	private void restoreDefaultSigintHandler() {
		if (terminal != null && previousSigintHandler != null) {
			LOGGER.fine("restoring default INT signal handler");
			terminal.handle(Terminal.Signal.INT, previousSigintHandler);
			previousSigintHandler = null;
		}
	}
}
