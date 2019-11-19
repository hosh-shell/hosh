package hosh.modules;

import hosh.spi.Channel;
import hosh.spi.ExitStatus;
import hosh.spi.Keys;
import hosh.spi.Records;
import hosh.spi.Values;
import org.jline.reader.History;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static hosh.testsupport.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

public class HistoryModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class NetworkTest {

		@Mock
		private Channel in;

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@Mock(stubOnly = true)
		private History history;

		@Mock(stubOnly = true)
		private History.Entry entry;

		@InjectMocks
		private HistoryModule.ListHistory sut;

		@Test
		public void noArgsEmptyHistory() {
			given(history.iterator()).willReturn(Collections.emptyListIterator());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void noArgsWithHistory() {
			given(history.iterator()).willReturn(Collections.singletonList(entry).listIterator());
			given(entry.time()).willReturn(Instant.EPOCH);
			given(entry.index()).willReturn(42);
			given(entry.line()).willReturn("cmd");
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Records.builder().entry(Keys.TIMESTAMP, Values.ofInstant(Instant.EPOCH)).entry(Keys.INDEX, Values.ofNumeric(42)).entry(Keys.TEXT, Values.ofText("cmd")).build());
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("whatever"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("no arguments expected")));
		}
	}

}
