package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AnsiFormatterTest {
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
		assertThat(result)
				.isEqualTo("1970-01-01T00:00:00 [[31mSEVERE[39m] [main] - [31mmessage[39m\norg.hosh.runtime.AnsiFormatterTest$Stacktraceless\n");
	}

	@Test
	public void info() {
		given(logRecord.getInstant()).willReturn(Instant.EPOCH);
		given(logRecord.getLevel()).willReturn(Level.INFO);
		given(logRecord.getMessage()).willReturn("message");
		String result = sut.format(logRecord);
		assertThat(result).isEqualTo("1970-01-01T00:00:00 [INFO] [main] - message\n");
	}

	@SuppressWarnings("serial")
	static class Stacktraceless extends RuntimeException {
		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
