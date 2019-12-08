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

import hosh.spi.ExitStatus;
import hosh.testsupport.WithThread;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutionException;

import static hosh.testsupport.ExitStatusAssert.assertThat;

@ExtendWith(MockitoExtension.class)
public class SupervisorTest {

	@RegisterExtension
	public final WithThread withThread = new WithThread();

	@InjectMocks
	private Supervisor sut;

	@AfterEach
	public void cleanup() {
		sut.close();
	}

	@Test
	public void noSubmit() throws ExecutionException {
		ExitStatus exitStatus = sut.waitForAll();
		assertThat(exitStatus).isSuccess();
	}

	@Test
	public void handleInterruptions() throws ExecutionException {
		sut.submit(() -> {
			Thread.sleep(10_000);
			return ExitStatus.success();
		});
		Thread.currentThread().interrupt(); // next call to Future.get() will throw InterruptedException
		ExitStatus waitForAll = sut.waitForAll();
		assertThat(waitForAll).isError();
	}

	@Test
	public void handleCancellations() throws ExecutionException {
		sut.submit(() -> {
			Thread.sleep(10_000);
			return ExitStatus.success();
		});
		sut.submit(() -> {
			Thread.sleep(10);
			raise("INT");
			return ExitStatus.success();
		});
		ExitStatus waitForAll = sut.waitForAll();
		assertThat(waitForAll).isError();
	}

	@Test
	public void allSubmitInSuccess() throws ExecutionException {
		sut.submit(() -> ExitStatus.success());
		sut.submit(() -> ExitStatus.success());
		ExitStatus exitStatus = sut.waitForAll();
		assertThat(exitStatus).isSuccess();
	}

	@Test
	public void oneSubmitInError() throws ExecutionException {
		sut.submit(() -> ExitStatus.success());
		sut.submit(() -> ExitStatus.error());
		ExitStatus exitStatus = sut.waitForAll();
		assertThat(exitStatus).isError();
	}

	// sun.misc.Signal is internal API without any replacement by now
	private static void raise(String signalName) throws ReflectiveOperationException {
		Class<?> signalClass = Class.forName("sun.misc.Signal");
		Object signal = signalClass.getConstructor(String.class).newInstance(signalName);
		signalClass.getMethod("raise", signalClass).invoke(null, signal);
	}

}
