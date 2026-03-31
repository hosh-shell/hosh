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

import hosh.spi.CommandArguments;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.test.support.TemporaryFolder;
import hosh.test.support.WithRandomTcpPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Unit tests for HttpServerModule.HttpServerCommand.
 */
class HttpServerModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class HttpServerCommandTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@RegisterExtension
		final WithRandomTcpPort withRandomTcpPort = new WithRandomTcpPort();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock
		State state;

		HttpServerModule.HttpServerCommand sut;

		@BeforeEach
		void createSut() {
			sut = new HttpServerModule.HttpServerCommand();
			sut.setState(state);
		}

		@Test
		void startStop() throws InterruptedException, IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			CountDownLatch countDownLatch = new CountDownLatch(1);
			sut.setBusyWait(new HttpServerModule.ExternalBusyWait(countDownLatch));
			String randomTcpPort = Integer.toString(withRandomTcpPort.getRandomTcpPort());
			CommandArguments commandArguments = CommandArguments.of(randomTcpPort, ".");
			Thread thread = Thread.ofVirtual().start(() -> {
				ExitStatus result = sut.run(commandArguments, in, out, err);
				assertThat(result).isSuccess();
			});
			// When
			HttpResponse<Void> result;
			try (HttpClient httpClient = HttpClient.newBuilder().build()) {
				HttpRequest request = HttpRequest.newBuilder()
						.GET()
						.uri(URI.create("http://localhost:" + randomTcpPort))
						.build();
				result = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
			}
			countDownLatch.countDown();
			thread.join();

			// Then
			assertThat(result.statusCode()).isEqualTo(200);
			then(in).shouldHaveNoInteractions();
			// one day... be more precise with the output
			then(out).should(Mockito.atLeastOnce()).send(any(Record.class), eq(EnumSet.of(OutputChannel.Option.DIRECT)));
			// there are no errors like "port already in use"
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void startStopStartWithSamePort() throws InterruptedException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			CountDownLatch countDownLatch = new CountDownLatch(1);
			sut.setBusyWait(new HttpServerModule.ExternalBusyWait(countDownLatch));
			String randomTcpPort = Integer.toString(withRandomTcpPort.getRandomTcpPort());
			CommandArguments commandArguments = CommandArguments.of(randomTcpPort, ".");

			// When, it is started for the first time
			Thread firstAttempt = Thread.ofVirtual().start(() -> {
				ExitStatus result = sut.run(commandArguments, in, out, err);
				assertThat(result).isSuccess();
			});
			countDownLatch.countDown();
			firstAttempt.join();
			// and it is started another time
			Thread secondAttempt = Thread.ofVirtual().start(() -> {
				ExitStatus result = sut.run(commandArguments, in, out, err);
				assertThat(result).isSuccess();
			});
			countDownLatch.countDown();
			secondAttempt.join();

			// Then
			then(in).shouldHaveNoInteractions();
			// be more precise with the output
			then(out).should(Mockito.atLeastOnce()).send(any(Record.class), eq(EnumSet.of(OutputChannel.Option.DIRECT)));
			// there are no errors like "port already in use"
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void noArgsReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: httpserver PORT DIRECTORY")));
		}

		@Test
		void oneArgReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("8080"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: httpserver PORT DIRECTORY")));
		}

		@Test
		void tooManyArgsReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("8080", "/tmp", "extra"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: httpserver PORT DIRECTORY")));
		}

		@Test
		void invalidPortReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("abc", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("port must be a number, got: abc")));
		}

		@Test
		void portZeroReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("0", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("port must be between 1 and 65535")));
		}

		@Test
		void portNegativeReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("-1", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("port must be between 1 and 65535")));
		}

		@Test
		void portTooLargeReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("65536", "/tmp"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("port must be between 1 and 65535")));
		}

		@Test
		void directoryNotFoundReturnsError() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());

			// When
			ExitStatus result = sut.run(CommandArguments.of("8080", "/nonexistent/directory"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("directory does not exist: /nonexistent/directory")));
		}

		@Test
		void pathIsFileReturnsError() throws Exception {
			// Given
			Path file = temporaryFolder.newFile("test.txt");

			// When
			ExitStatus result = sut.run(CommandArguments.of("8080", file.toString()), in, out, err);

			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("directory does not exist: " + file)));
		}

		@Test
		void portOneIsValidPortNumber() {
			// Given - port 1 is valid, but /tmp may not exist on test machine
			// or may not be a directory
			given(state.getCwd()).willReturn(temporaryFolder.toPath());

			// When
			ExitStatus result = sut.run(CommandArguments.of("1", "/nonexistent"), in, out, err);

			// Then - error should be about directory, not port
			assertThat(result).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("directory does not exist: /nonexistent")));
		}

		@Test
		void port65535IsValidPortNumber() {
			// Given - port 65535 is valid
			given(state.getCwd()).willReturn(temporaryFolder.toPath());

			// When
			ExitStatus result = sut.run(CommandArguments.of("65535", "/nonexistent"), in, out, err);

			// Then - error should be about directory, not port
			assertThat(result).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("directory does not exist: /nonexistent")));
		}

		@Test
		void portOneBoundary() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());

			// When
			ExitStatus result = sut.run(CommandArguments.of("1", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("directory does not exist: /nonexistent")));
		}

		@Test
		void emptyPortStringReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("port must be a number, got: ")));
		}

		@Test
		void whitespacePortReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("   ", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("port must be a number, got:    ")));
		}

		@Test
		void portWithLeadingZeros() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("0008080", "/nonexistent"), in, out, err);

			// Then - should parse as 8080 and fail on directory
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("directory does not exist: /nonexistent")));
		}

		@Test
		void floatingPointPortReturnsError() {
			// Given

			// When
			ExitStatus result = sut.run(CommandArguments.of("8080.5", "/nonexistent"), in, out, err);

			// Then
			assertThat(result).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("port must be a number, got: 8080.5")));
		}

		@Test
		void portWithPlusSignReturnsError() {
			// Given - Note: "+8080" actually parses as 8080 in Integer.parseInt()
			// So this test will fail on directory not existing, which is correct behavior
			given(state.getCwd()).willReturn(temporaryFolder.toPath());

			// When
			ExitStatus result = sut.run(CommandArguments.of("+8080", "/nonexistent"), in, out, err);

			// Then - "+8080" is accepted as valid port 8080
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("directory does not exist: /nonexistent")));
		}

	}
}
