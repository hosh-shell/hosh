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
package hosh.modules.httpserver;

import hosh.doc.Todo;
import hosh.spi.CommandArguments;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.Values;
import hosh.test.support.TemporaryFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Unit tests for HttpServerModule.HttpServerCommand.
 */
class HttpServerModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class HttpServerCommandTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock
		hosh.spi.State state;

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		HttpServerModule.HttpServerCommand sut;

		@BeforeEach
		void createSut() {
			sut = new HttpServerModule.HttpServerCommand();
			sut.setState(state);
		}

		@Todo(description = "the happy path")
		@Test
		void startStop() {

		}

		@Todo(description = "check proper resource handling (i.e. no java.net.BindException: Address already in use")
		@Test
		void startStopStartWithSamePort() {

		}

		// Error path: no arguments
		@Test
		void noArgsReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR, Values.ofText("usage: httpserver PORT DIRECTORY")));
		}

		// Error path: one argument
		@Test
		void oneArgReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("8080"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR, Values.ofText("usage: httpserver PORT DIRECTORY")));
		}

		// Error path: too many arguments
		@Test
		void tooManyArgsReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("8080", "/tmp", "extra"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR, Values.ofText("usage: httpserver PORT DIRECTORY")));
		}

		// Error path: non-numeric port
		@Test
		void invalidPortReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("abc", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR, Values.ofText("port must be a number, got: abc")));
		}

		// Error path: port zero
		@Test
		void portZeroReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("0", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be between 1 and 65535")));
		}

		// Error path: port negative
		@Test
		void portNegativeReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("-1", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be between 1 and 65535")));
		}

		// Error path: port too large
		@Test
		void portTooLargeReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("65536", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be between 1 and 65535")));
		}

		// Error path: port way too large
		@Test
		void portMaxIntReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("2147483647", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be between 1 and 65535")));
		}

		// Error path: directory does not exist
		@Test
		void directoryNotFoundReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("8080", "/nonexistent/directory"),
					in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: /nonexistent/directory")));
		}

		// Error path: path is a file, not a directory
		@Test
		void pathIsFileReturnsError() throws Exception {
			// Given
			Path file = temporaryFolder.newFile("test.txt");

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("8080", file.toString()),
					in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: " + file)));
		}

		// Boundary: minimum valid port
		// Note: cannot test actual server startup in unit tests (would need integration test)
		// This test verifies port 1 doesn't fail port validation
		@Test
		void portOneIsValidPortNumber() {
			// Given - port 1 is valid, but /tmp may not exist on test machine
			// or may not be a directory

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("1", "/nonexistent"), in, out, err);

			// Then - error should be about directory, not port
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: /nonexistent")));
		}

		// Boundary: maximum valid port
		@Test
		void port65535IsValidPortNumber() {
			// Given - port 65535 is valid

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("65535", "/nonexistent"), in, out, err);

			// Then - error should be about directory, not port
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: /nonexistent")));
		}

		// Boundary: port at lower limit of valid range
		@Test
		void portOneBoundary() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("1", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: /nonexistent")));
		}

		// Boundary: port at upper limit of valid range
		@Test
		void port65535Boundary() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("65535", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: /nonexistent")));
		}

		// Boundary: empty port string should fail parsing
		@Test
		void emptyPortStringReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be a number, got: ")));
		}

		// Boundary: whitespace port should fail
		@Test
		void whitespacePortReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("   ", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be a number, got:    ")));
		}

		// Edge case: leading zeros should parse fine
		@Test
		void portWithLeadingZeros() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("0008080", "/nonexistent"), in, out, err);

			// Then - should parse as 8080 and fail on directory
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: /nonexistent")));
		}

		// Edge case: floating point port should fail
		@Test
		void floatingPointPortReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("8080.5", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be a number, got: 8080.5")));
		}

		// Edge case: port with plus sign should fail
		@Test
		void portWithPlusSignReturnsError() {
			// Given - Note: "+8080" actually parses as 8080 in Integer.parseInt()
			// So this test will fail on directory not existing, which is correct behavior

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("+8080", "/nonexistent"), in, out, err);

			// Then - "+8080" is accepted as valid port 8080
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("directory does not exist: /nonexistent")));
		}

		// Edge case: hexadecimal port should fail
		@Test
		void hexPortReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(
					CommandArguments.of("0x1F90", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(err).should().send(Records.singleton(
					Keys.ERROR,
					Values.ofText("port must be a number, got: 0x1F90")));
		}
	}
}
