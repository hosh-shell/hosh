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
package hosh.modules.network;

import com.sun.net.httpserver.HttpServer;
import hosh.spi.CommandArguments;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import hosh.spi.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

/**
 * Integration tests against the JDK embedded HTTP server.
 */
@ExtendWith(MockitoExtension.class)
class HttpWithEmbeddedServerTest {

	@Mock
	InputChannel in;

	@Mock
	OutputChannel out;

	@Mock
	OutputChannel err;

	@Captor
	ArgumentCaptor<Record> body;

	NetworkModule.Http sut;

	HttpServer server;

	@BeforeEach
	void setUp() throws IOException {
		sut = new NetworkModule.Http();
		sut.setVersion(new Version("v1.2.3"));
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.start();
	}

	@AfterEach
	void tearDown() {
		server.stop(0);
	}

	@Test
	void ok() {
		// Given
		server.createContext("/path", exchange -> {
			byte[] response = "line1\nline2".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response);
			}
		});
		// When
		String url = String.format("http://localhost:%d/path", server.getAddress().getPort());
		ExitStatus result = sut.run(CommandArguments.of(url), in, out, err);
		// Then
		assertThat(result).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).should(atLeastOnce()).send(body.capture());
		then(err).shouldHaveNoInteractions();
		assertThat(body.getAllValues()).containsExactly(
				Records.singleton(Keys.TEXT, Values.ofText("line1")),
				Records.singleton(Keys.TEXT, Values.ofText("line2")));
	}

	@Test
	void notFound() {
		// Given
		// no handler registered for /not-found — server returns 404
		// When
		String url = String.format("http://localhost:%d/not-found", server.getAddress().getPort());
		ExitStatus result = sut.run(CommandArguments.of(url), in, out, err);
		// Then
		assertThat(result).isError();
		then(in).shouldHaveNoInteractions();
		then(out).shouldHaveNoInteractions();
	}
}
