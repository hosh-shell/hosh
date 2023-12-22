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
package hosh.modules.formatting;

import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.test.support.TemporaryFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class FormattingModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class CsvTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FormattingModule.Csv sut;

		@BeforeEach
		void createSut() {
			sut = new FormattingModule.Csv();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			// When
			ExitStatus result = sut.run(List.of(), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: csv write|read file")));
		}

		@Test
		void writeEmpty() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(Optional.empty());

			// When
			ExitStatus result = sut.run(List.of("write", "file.csv"), in, out, err);

			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void writeOneRecord() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(Optional.of(Records.singleton(Keys.SIZE, Values.ofSize(1))), Optional.empty());

			// When
			ExitStatus result = sut.run(List.of("write", "file.csv"), in, out, err);

			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

	}
}
