/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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

import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.then;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests against postman-echo.
 */
@ExtendWith(MockitoExtension.class)
class PostmanHttpTest {

	@Mock
	InputChannel in;

	@Mock
	OutputChannel out;

	@Mock
	OutputChannel err;

	@Captor
	ArgumentCaptor<Record> body;

	NetworkModule.Http sut;

	@BeforeEach
	void createSut() {
		sut = new NetworkModule.Http();
	}

	@Test
	void getOk() {
		// Given
		URI uri = URI.create("https://postman-echo.com/get?param=42");
		// When
		ExitStatus exitStatus = sut.run(List.of(uri.toString()), in, out, err);
		// Then
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).should().send(body.capture());
		then(err).shouldHaveNoInteractions();
		JsonObject result = JsonHelpers.parse(body.getValue());
		assertThat(result.getJsonObject("args").getString("param")).isEqualTo("42");
		assertThat(result.getJsonObject("headers").getJsonString("user-agent").getString()).isEqualTo("Hosh");
	}

	@Test
	void get404() {
		// Given
		URI uri = URI.create("https://postman-echo.com/status/404");
		// When
		ExitStatus exitStatus = sut.run(List.of(uri.toString()), in, out, err);
		// Then
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).should().send(body.capture());
		then(err).shouldHaveNoInteractions();
		JsonObject result = JsonHelpers.parse(body.getValue());
		assertThat(result.getJsonNumber("status").intValue()).isEqualTo(404);
	}

	// helpers
	static class JsonHelpers {

		static final Logger LOGGER = LoggerFactory.forEnclosingClass();

		static JsonObject parse(Record record) {
			JsonReaderFactory readerFactory = Json.createReaderFactory(Map.of());
			String body = record.value(Keys.TEXT).flatMap(x -> x.unwrap(String.class)).orElseThrow();
			LOGGER.log(Level.INFO, () -> String.format("body is: '%s'", body));
			return readerFactory.createReader(new StringReader(body)).readObject();
		}
	}
}
