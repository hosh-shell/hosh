/*
 * MIT License
 *
 * Copyright (c) 2019-2021 Davide Angelocola
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

import hosh.spi.Ansi;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AutoTableChannelTest {

	@Mock
	OutputChannel out;

	@Captor
	ArgumentCaptor<Record> records;

	AutoTableChannel sut;

	@BeforeEach
	void createSut() {
		sut = new AutoTableChannel(out);
	}

	@Test
	void tableWithNoRecords() {
		sut.end();
		then(out).shouldHaveNoInteractions();
	}

	@Test
	void tableWithColumnLongerThanValues() {
		Record record = Records.builder().entry(Keys.COUNT, Values.ofNumeric(2)).entry(Keys.TEXT, Values.ofText("whatever")).build();
		sut.send(record);
		sut.end();
		then(out).should(times(2)).send(records.capture());
		Assertions.assertThat(records.getAllValues()).containsExactly(
			Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("count  text      "), Ansi.Style.FG_MAGENTA)),
			Records.singleton(Keys.TEXT, /*            */ Values.ofText("2      whatever  ")));
	}

	@Test
	void tableWithColumnShorterThanValues() {
		Record record = Records.builder().entry(Keys.COUNT, Values.ofNumeric(2)).entry(Keys.TEXT, Values.ofText("aa")).build();
		sut.send(record);
		sut.end();
		then(out).should(times(2)).send(records.capture());
		Assertions.assertThat(records.getAllValues()).containsExactly(
			Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("count  text  "), Ansi.Style.FG_MAGENTA)),
			Records.singleton(Keys.TEXT, /*            */ Values.ofText("2      aa    ")));
	}

	@Test
	void tableWithNone() {
		Record record = Records.builder().entry(Keys.COUNT, Values.none()).entry(Keys.TEXT, Values.ofText("whatever")).build();
		sut.send(record);
		sut.end();
		then(out).should(times(2)).send(records.capture());
		Assertions.assertThat(records.getAllValues()).containsExactly(
			Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("count  text      "), Ansi.Style.FG_MAGENTA)),
			Records.singleton(Keys.TEXT, /*            */ Values.ofText("       whatever  ")));
	}

	@Test
	void nonOverflow() {
		Record record = Records.builder().entry(Keys.COUNT, Values.none()).entry(Keys.TEXT, Values.ofText("whatever")).build();
		for (int i = 1; i < AutoTableChannel.OVERFLOW; i++) {
			sut.send(record);
		}
		then(out).should(never()).send(records.capture());
		sut.end();
		then(out).should(times(AutoTableChannel.OVERFLOW)).send(records.capture());
		Assertions.assertThat(records.getAllValues()).containsOnly(
			Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("count  text      "), Ansi.Style.FG_MAGENTA)),
			Records.singleton(Keys.TEXT, /*            */ Values.ofText("       whatever  ")));
	}

	@Test
	void overflow() {
		Record record = Records.builder().entry(Keys.COUNT, Values.none()).entry(Keys.TEXT, Values.ofText("whatever")).build();
		for (int i = 1; i <= AutoTableChannel.OVERFLOW + 1; i++) {
			sut.send(record);
		}
		then(out).should(times(AutoTableChannel.OVERFLOW + 1)).send(records.capture());
		sut.end();
		then(out).shouldHaveNoMoreInteractions();
	}

}