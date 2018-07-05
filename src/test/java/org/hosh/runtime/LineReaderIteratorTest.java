package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.NoSuchElementException;

import org.hosh.spi.State;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class LineReaderIteratorTest {

	@Mock
	private LineReader lineReader;

	@Mock
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
}
