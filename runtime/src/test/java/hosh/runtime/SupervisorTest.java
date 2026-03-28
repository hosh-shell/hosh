/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
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
import hosh.test.support.WithThread;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupervisorTest {

	@RegisterExtension
	final WithThread withThread = new WithThread();

	@Mock
	Terminal terminal;

	Supervisor sut;

	@BeforeEach
	void createSut() {
		sut = new Supervisor();
	}

	@AfterEach
	void cleanup() {
		sut.close();
	}

	@Test
	void noSubmit() throws ExecutionException {
		// Given

		// When
		ExitStatus result = sut.waitForAll();

		// Then
		assertThat(result).isSuccess();
	}

	@Test
	void handleInterruptions() throws ExecutionException {
		// Given
		sut.submit(() -> {
			Thread.sleep(Duration.ofSeconds(10));
			return ExitStatus.success();
		});
		withThread.interrupt(); // next call to Future.get() will throw InterruptedException

		// When
		ExitStatus waitForAll = sut.waitForAll();

		// Then
		assertThat(waitForAll).isError();
	}

	@Test
	void allSubmitInSuccess() throws ExecutionException {
		// Given
		sut.submit(ExitStatus::success);
		sut.submit(ExitStatus::success);

		// When
		ExitStatus result = sut.waitForAll();

		// Then
		assertThat(result).isSuccess();
	}

	@Test
	void oneSubmitInError() throws ExecutionException {
		// Given
		sut.submit(ExitStatus::success);
		sut.submit(ExitStatus::error);

		// When
		ExitStatus result = sut.waitForAll();

		// Then
		assertThat(result).isError();
	}

	@Test
	void sigintCancelsPendingTasks() throws Exception {
		// Given - a Supervisor with terminal-based SIGINT handling, and a task that blocks until interrupted
		ArgumentCaptor<Terminal.SignalHandler> handlerCaptor = ArgumentCaptor.forClass(Terminal.SignalHandler.class);
		when(terminal.handle(eq(Terminal.Signal.INT), handlerCaptor.capture())).thenReturn(Terminal.SignalHandler.SIG_DFL);
		CountDownLatch taskStarted = new CountDownLatch(1);
		CountDownLatch taskCancelled = new CountDownLatch(1);
		try (Supervisor sutWithTerminal = new Supervisor(terminal)) {
			sutWithTerminal.submit(() -> {
				taskStarted.countDown();
				try {
					Thread.sleep(Duration.ofSeconds(10));
				} catch (InterruptedException e) {
					taskCancelled.countDown();
					Thread.currentThread().interrupt();
				}
				return ExitStatus.success();
			});
			taskStarted.await();
			// When - waitForAll installs the handler; run it in background, then simulate SIGINT
			Thread waiter = Thread.ofVirtual().start(() -> {
				try {
					sutWithTerminal.waitForAll();
				} catch (ExecutionException e) {
					Thread.currentThread().interrupt();
				}
			});
			Thread.sleep(100); // let waitForAll install the handler
			Terminal.SignalHandler handler = handlerCaptor.getValue();
			assertThat(handler).as("SIGINT handler must be installed by waitForAll").isNotNull();
			handler.handle(Terminal.Signal.INT);
			waiter.join(1_000);
		}
		// Then - the task was cancelled by the handler
		assertThat(taskCancelled.await(1, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void concurrentSubmit() throws ExecutionException, InterruptedException {
		// Given - many virtual threads submitting tasks simultaneously
		int count = 100;
		CountDownLatch ready = new CountDownLatch(count);
		CountDownLatch go = new CountDownLatch(1);
		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			threads.add(Thread.ofVirtual().start(() -> {
				ready.countDown();
				try {
					go.await();
					sut.submit(ExitStatus::success);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}));
		}
		// When
		ready.await();
		go.countDown();
		for (Thread t : threads) {
			t.join();
		}
		ExitStatus result = sut.waitForAll();
		// Then
		assertThat(result).isSuccess();
	}

}
