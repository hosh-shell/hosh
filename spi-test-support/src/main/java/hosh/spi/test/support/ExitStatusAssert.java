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
package hosh.spi.test.support;

import hosh.spi.ExitStatus;
import org.assertj.core.api.AbstractAssert;

public class ExitStatusAssert extends AbstractAssert<ExitStatusAssert, ExitStatus> {

	public static ExitStatusAssert assertThat(ExitStatus actual) {
		return new ExitStatusAssert(actual);
	}

	private ExitStatusAssert(ExitStatus actual) {
		super(actual, ExitStatusAssert.class);
	}

	public void isSuccess() {
		isNotNull();
		if (actual.isError()) {
			failWithMessage("expected success but was error");
		}
	}

	public void isError() {
		isNotNull();
		if (actual.isSuccess()) {
			failWithMessage("expected error but was success");
		}
	}

	public void hasExitCode(int expectedCode) {
		isNotNull();
		if (expectedCode != actual.value()) {
			failWithMessage("expected %d but as %d", expectedCode, actual.value());
		}
	}
}
