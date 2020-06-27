package hosh.runtime;

import hosh.spi.Ansi;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AutoTableChannelTest {

	@Mock
	private OutputChannel out;

	@InjectMocks
	private AutoTableChannel sut;

	@Captor
	private ArgumentCaptor<Record> records;

	@Test
	public void tableWithNoRecords() {
		sut.end();
		then(out).shouldHaveNoInteractions();
	}

	@Test
	public void tableWithColumnLongerThanValues() {
		Record record1 = Records.builder().entry(Keys.COUNT, Values.ofNumeric(2)).entry(Keys.TEXT, Values.ofText("whatever")).build();
		sut.send(record1);
		sut.end();
		then(out).should(times(2)).send(records.capture());
		Assertions.assertThat(records.getAllValues()).containsExactly(
			Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("count  text      "), Ansi.Style.FG_MAGENTA)),
			Records.singleton(Keys.TEXT, /*            */ Values.ofText("2      whatever  ")));
	}

	@Test
	public void tableWithColumnShorterThanValues() {
		Record record1 = Records.builder().entry(Keys.COUNT, Values.ofNumeric(2)).entry(Keys.TEXT, Values.ofText("aa")).build();
		sut.send(record1);
		sut.end();
		then(out).should(times(2)).send(records.capture());
		Assertions.assertThat(records.getAllValues()).containsExactly(
			Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("count  text  "), Ansi.Style.FG_MAGENTA)),
			Records.singleton(Keys.TEXT, /*            */ Values.ofText("2      aa    ")));
	}

	@Test
	public void tableWithNone() {
		Record record1 = Records.builder().entry(Keys.COUNT, Values.none()).entry(Keys.TEXT, Values.ofText("whatever")).build();
		sut.send(record1);
		sut.end();
		then(out).should(times(2)).send(records.capture());
		Assertions.assertThat(records.getAllValues()).containsExactly(
			Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("count  text      "), Ansi.Style.FG_MAGENTA)),
			Records.singleton(Keys.TEXT, /*            */ Values.ofText("       whatever  ")));
	}

}