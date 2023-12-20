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

import hosh.test.support.WithThread;
import hosh.test.support.WithTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serial;
import java.time.Instant;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HoshFormatterTest {

	@RegisterExtension
	final WithThread withThread = new WithThread();

	@RegisterExtension
	final WithTimeZone withTimeZone = new WithTimeZone();

	@Mock(stubOnly = true)
	LogRecord logRecord;

	HoshFormatter sut;

	@BeforeEach
	void createSut() {
		sut = new HoshFormatter();
	}

	@Test
	void severeWithStacktrace() {
		withTimeZone.changeTo(TimeZone.getTimeZone("Etc/GMT+8"));
		withThread.renameTo("main");
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.SEVERE);
		given(logRecord.getMessage()).willReturn("message");
		given(logRecord.getThrown()).willReturn(new StackTraceLess());
		String result = sut.format(logRecord);
		String expected = "1969-12-31T16:00:00.000 [SEVERE] [main] - message\nhosh.runtime.HoshFormatterTest$StackTraceLess\n";
		assertThat(result).isEqualToNormalizingNewlines(expected);
	}

	@Test
	void info() {
		withTimeZone.changeTo(TimeZone.getTimeZone("Etc/GMT+8"));
		withThread.renameTo("main");
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.INFO);
		given(logRecord.getMessage()).willReturn("message");
		String result = sut.format(logRecord);
		assertThat(result).isEqualToNormalizingNewlines("1969-12-31T16:00:00.000 [INFO] [main] - message\n");
	}

	@Test
	void skipThreadNameForHttpClient() {
		withTimeZone.changeTo(TimeZone.getTimeZone("Etc/GMT+8"));
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.INFO);
		given(logRecord.getMessage()).willReturn("message");
		given(logRecord.getLoggerName()).willReturn("jdk.internal.httpclient");
		String result = sut.format(logRecord);
		assertThat(result).isEqualToNormalizingNewlines("1969-12-31T16:00:00.000 [INFO] - message\n");
	}

	static class StackTraceLess extends RuntimeException {

		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
