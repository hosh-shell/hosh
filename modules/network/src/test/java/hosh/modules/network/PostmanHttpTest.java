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

import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static hosh.test.support.ExitStatusAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

/**
 * Integration tests against postman-echo.
 */
@ExtendWith(MockitoExtension.class)
public class PostmanHttpTest {

	@Mock
	private InputChannel in;

	@Mock
	private OutputChannel out;

	@Mock
	private OutputChannel err;

	@InjectMocks
	private NetworkModule.Http sut;

	@Test
	public void getOk() {
		// Given
		URI uri = URI.create("https://postman-echo.com/get");
		// When
		ExitStatus exitStatus = sut.run(List.of(uri.toString()), in, out, err);
		// Then
		assertThat(exitStatus).isSuccess();
		then(in).shouldHaveNoInteractions();
		then(out).should().send(any());
		then(err).shouldHaveNoInteractions();
	}

}
