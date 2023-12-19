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
package hosh.spi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorsTest {

	@Test
	void exceptionWithoutMessage() {
		Exception npe = new NullPointerException();
		Record result = Errors.message(npe);
		assertThat(result).isEqualTo(Records.singleton(Keys.ERROR, Values.ofText("(no message)")));
	}

	@Test
	void exceptionWithMessage() {
		Exception npe = new IllegalArgumentException("whatever");
		Record result = Errors.message(npe);
		assertThat(result).isEqualTo(Records.singleton(Keys.ERROR, Values.ofText("whatever")));
	}

	@Test
	void messageFormat() {
		Record result = Errors.message("value is %d", 1);
		assertThat(result).isEqualTo(Records.singleton(Keys.ERROR, Values.ofText("value is 1")));
	}

	@Test
	void usage() {
		Record result = Errors.usage("cmd [option]");
		assertThat(result).isEqualTo(Records.singleton(Keys.ERROR, Values.ofText("usage: cmd [option]")));
	}
}