/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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

import hosh.doc.Todo;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import hosh.test.support.WithThread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Stream;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.assertj.core.api.Assertions.assertThat;

class NetworkModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class NetworkTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		NetworkModule.Network sut;

		@BeforeEach
		void setUp() {
			sut = new NetworkModule.Network();
		}

		@Todo(description = "this is a very bland test: let's try to consolidate this command before investing more")
		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(Mockito.any(Record.class));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("whatever"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: network")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class HttpTest {

		@RegisterExtension
		final WithThread withThread = new WithThread();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		NetworkModule.Http.Requestor requestor;

		@Mock(stubOnly = true)
		HttpResponse<Stream<String>> response;

		NetworkModule.Http sut;

		@BeforeEach
		void setUp() {
			sut = new NetworkModule.Http();
			sut.setRequestor(requestor);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: http URL")));
		}

		@Test
		void oneArg() throws InterruptedException {
			given(requestor.send(Mockito.any())).willReturn(response);
			given(response.body()).willReturn(Stream.of("line1"));
			ExitStatus exitStatus = sut.run(List.of("https://example.org"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("line1")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void interrupted() throws InterruptedException {
			given(requestor.send(Mockito.any())).willThrow(new InterruptedException("simulated"));
			ExitStatus exitStatus = sut.run(List.of("https://example.org"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("interrupted")));
			assertThat(withThread.isInterrupted()).isTrue();
		}
	}
}
