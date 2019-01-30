package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.io.IOError;
import java.io.InterruptedIOException;
import java.util.NoSuchElementException;

import org.hosh.spi.State;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LineReaderIteratorTest {
	@Mock(stubOnly = true)
	private LineReader lineReader;
	@Mock(stubOnly = true)
	private State state;
	@InjectMocks
	private LineReaderIterator sut;

	@Test
	public void oneLine() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willReturn("1");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("1");
	}

	@Test
	public void twoLines() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willReturn("1", "2");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("1");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("2");
	}

	@Test
	public void hasNextIsIdempotent() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willReturn("1");
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.hasNext()).isTrue(); // second call
		assertThat(sut.next()).isEqualTo("1");
	}

	@Test
	public void stopsAtEOF() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willThrow(new EndOfFileException("simulated EOF"));
		assertThat(sut.hasNext()).isFalse();
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> sut.next());
	}

	@Test
	public void killsCurrentLineAtINT() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willThrow(new UserInterruptException("simulated INT"));
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("");
	}

	@Test
	public void fixRaceConditionInJLines() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willThrow(new IOError(new InterruptedIOException("simulated INT")));
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("");
	}

	@Test(expected = IOError.class)
	public void stopsAtGenericIOError() throws Exception {
		given(state.getId()).willReturn(0);
		given(lineReader.readLine(anyString())).willThrow(new IOError(new IllegalArgumentException("simulated exception")));
		assertThat(sut.hasNext()).isTrue();
		assertThat(sut.next()).isEqualTo("");
	}
}
