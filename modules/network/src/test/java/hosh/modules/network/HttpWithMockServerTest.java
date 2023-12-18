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
package hosh.modules.network;

import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpResponse;

import java.util.List;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockserver.model.HttpRequest.request;

/**
 * Integration tests against a locally running mock-server.
 */
@ExtendWith({MockitoExtension.class, MockServerExtension.class})
class HttpWithMockServerTest {

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
    void ok(ClientAndServer clientAndServer) {
        // Given
        clientAndServer.when(
                        request().withMethod("GET").withPath("/path")
                )
                .respond(
                        HttpResponse.response().withStatusCode(200)
                                .withBody("line1\nline2")
                );
        // When
        String arg = String.format("http://localhost:%d/path", clientAndServer.getLocalPort());
        ExitStatus exitStatus = sut.run(List.of(arg), in, out, err);
        // Then
        assertThat(exitStatus).isSuccess();
        then(in).shouldHaveNoInteractions();
        then(out).should(atLeastOnce()).send(body.capture());
        then(err).shouldHaveNoInteractions();
        assertThat(body.getAllValues()).containsExactly(Records.singleton(Keys.TEXT, Values.ofText("line1")), Records.singleton(Keys.TEXT, Values.ofText("line2")));
    }

    @Test
    void notFound(ClientAndServer clientAndServer) {
        // Given
        // no url is expected in the mockserver
        // When
        String arg = String.format("http://localhost:%d/not-found", clientAndServer.getLocalPort());
        ExitStatus exitStatus = sut.run(List.of(arg), in, out, err);
        // Then
        assertThat(exitStatus).isError();
        then(in).shouldHaveNoInteractions();
        then(out).shouldHaveNoInteractions();
        then(err).shouldHaveNoInteractions();
    }

}
