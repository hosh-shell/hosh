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
package hosh.modules.parquet;

import hosh.modules.parquet.ParquetModule.FromParquet;
import hosh.modules.parquet.ParquetModule.ToParquet;
import hosh.spi.CommandArguments;
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

import java.net.URISyntaxException;
import java.nio.file.Path;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

class ParquetModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class FromParquetTest {

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

		FromParquet sut;

		@BeforeEach
		void createSut() {
			sut = new FromParquet();
			sut.setState(state);
		}

		@Test
		void missingArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-parquet file")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("a.parquet", "b.parquet"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-parquet file")));
		}

		@Test
		void fileNotFound() {
			// Given
			Path missing = temporaryFolder.toPath().resolve("missing.parquet").toAbsolutePath();
			// When
			ExitStatus result = sut.run(CommandArguments.of(missing.toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("file not found: " + missing)));
		}

		@Test
		void notAFile() {
			// Given
			Path dir = temporaryFolder.toPath().toAbsolutePath();
			// When
			ExitStatus result = sut.run(CommandArguments.of(dir.toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a regular file: " + dir)));
		}

		@Test
		void readRecords() throws URISyntaxException {
			// Given
			Path fixture = Path.of(getClass().getResource("test.parquet").toURI());
			// When
			ExitStatus result = sut.run(CommandArguments.of(fixture.toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("alice")).entry(Keys.of("age"), Values.ofNumeric(30)).build());
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("bob")).entry(Keys.of("age"), Values.ofNumeric(25)).build());
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ToParquetTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		ToParquet sut;

		@BeforeEach
		void createSut() {
			sut = new ToParquet();
		}

		@Test
		void notYetImplemented() {
			assertThatThrownBy(() -> sut.run(CommandArguments.of("output.parquet"), in, out, err))
					.isInstanceOf(UnsupportedOperationException.class)
					.hasMessage("to-parquet: write support not yet available in hardwood");
		}
	}
}
