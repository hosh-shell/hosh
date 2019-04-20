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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.hosh.testsupport.WithTimeZone;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AnsiFormatterTest {

	@Rule
	public final WithTimeZone withTimeZone = new WithTimeZone(TimeZone.getTimeZone("PST"));

	@Mock(stubOnly = true)
	private LogRecord logRecord;

	@InjectMocks
	private AnsiFormatter sut;

	@Test
	public void severeWithStacktrace() {
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.SEVERE);
		given(logRecord.getMessage()).willReturn("message");
		given(logRecord.getThrown()).willReturn(new Stacktraceless());
		String result = sut.format(logRecord);
		assertThat(result).isEqualToNormalizingNewlines(
				"1969-12-31T16:00:00.000 [[31mSEVERE[39m] [main] - [31mmessage[39m\norg.hosh.runtime.AnsiFormatterTest$Stacktraceless\n");
	}

	@Test
	public void warning() {
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.WARNING);
		given(logRecord.getMessage()).willReturn("message");
		String result = sut.format(logRecord);
		assertThat(result).isEqualToNormalizingNewlines("1969-12-31T16:00:00.000 [[33mWARNING[39m] [main] - [33mmessage[39m\n");
	}

	@Test
	public void info() {
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.INFO);
		given(logRecord.getMessage()).willReturn("message");
		String result = sut.format(logRecord);
		assertThat(result).isEqualToNormalizingNewlines("1969-12-31T16:00:00.000 [INFO] [main] - message\n");
	}

	@SuppressWarnings("serial")
	static class Stacktraceless extends RuntimeException {

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
