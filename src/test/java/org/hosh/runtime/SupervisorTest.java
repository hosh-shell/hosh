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

import static org.hosh.testsupport.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.then;

import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.Records;
import org.hosh.spi.Values;
import org.hosh.testsupport.SneakySignal;
import org.hosh.testsupport.WithThread;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SupervisorTest {

	@RegisterExtension
	public final WithThread withThread = new WithThread();

	@Mock
	private Channel err;

	@InjectMocks
	private Supervisor sut;

	@AfterEach
	public void cleanup() {
		sut.close();
	}

	@Test
	public void noSubmit() {
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus).isSuccess();
	}

	@Test
	public void handleSignals() {
		sut.submit(() -> {
			SneakySignal.raise("INT");
			return ExitStatus.success();
		});
		ExitStatus waitForAll = sut.waitForAll(err);
		assertThat(waitForAll).isError();
	}

	@Test
	public void handleInterruptions() {
		sut.submit(() -> {
			Thread.sleep(10_000);
			return ExitStatus.success();
		});
		Thread.currentThread().interrupt(); // next call to Future.get() will throw InterruptedException
		ExitStatus waitForAll = sut.waitForAll(err);
		assertThat(waitForAll).isError();
	}

	@Test
	public void allSubmitInSuccess() {
		sut.submit(() -> ExitStatus.success());
		sut.submit(() -> ExitStatus.success());
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus).isSuccess();
	}

	@Test
	public void oneSubmitInError() {
		sut.submit(() -> ExitStatus.success());
		sut.submit(() -> ExitStatus.error());
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus).isError();
	}

	@Test
	public void oneSubmitWithException() {
		sut.submit(() -> {
			throw new NullPointerException("simulated error");
		});
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus).isError();
		then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("simulated error")));
	}

	@Test
	public void oneSubmitWithExceptionButNoMessage() {
		sut.submit(() -> {
			throw new NullPointerException();
		});
		ExitStatus exitStatus = sut.waitForAll(err);
		assertThat(exitStatus).isError();
		then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("(no message provided)")));
	}
}
