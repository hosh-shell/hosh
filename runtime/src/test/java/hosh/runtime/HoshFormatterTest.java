/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class HoshFormatterTest {

	@RegisterExtension
	public final WithThread withThread = new WithThread();

	@RegisterExtension
	public final WithTimeZone withTimeZone = new WithTimeZone(TimeZone.getTimeZone("Etc/GMT+8"));

	@Mock(stubOnly = true)
	private LogRecord logRecord;

	@InjectMocks
	private HoshFormatter sut;

	@Test
	public void severeWithStacktrace() {
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
	public void info() {
		withThread.renameTo("main");
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.INFO);
		given(logRecord.getMessage()).willReturn("message");
		String result = sut.format(logRecord);
		assertThat(result).isEqualToNormalizingNewlines("1969-12-31T16:00:00.000 [INFO] [main] - message\n");
	}

	@Test
	public void skipThreadNameForHttpClient() {
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.INFO);
		given(logRecord.getMessage()).willReturn("message");
		given(logRecord.getLoggerName()).willReturn("jdk.internal.httpclient");
		String result = sut.format(logRecord);
		assertThat(result).isEqualToNormalizingNewlines("1969-12-31T16:00:00.000 [INFO] - message\n");
	}

	@SuppressWarnings("serial")
	static class StackTraceLess extends RuntimeException {

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
