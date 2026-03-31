/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
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
package hosh.modules.history;

import hosh.spi.Values;
import hosh.spi.CommandArguments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import org.jline.reader.History;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class HistoryModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ListHistoryTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		History history;

		@Mock(stubOnly = true)
		History.Entry entry;

		HistoryModule.ListHistory sut;

		@BeforeEach
		void createSut() {
			sut = new HistoryModule.ListHistory();
			sut.setHistory(history);
		}

		@Test
		void noArgsEmptyHistory() {
			// Given
			given(history.iterator()).willReturn(Collections.emptyListIterator());

			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);

			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void noArgsWithHistory() {
			// Given
			given(history.iterator()).willReturn(Collections.singletonList(entry).listIterator());
			given(entry.time()).willReturn(Instant.EPOCH);
			given(entry.line()).willReturn("cmd");

			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);

			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.TIMESTAMP, Values.ofInstant(Instant.EPOCH))
					.entry(Keys.TEXT, Values.ofText("cmd"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArg() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("whatever"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: history")));
		}
	}
}
