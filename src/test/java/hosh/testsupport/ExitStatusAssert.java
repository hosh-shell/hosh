/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package hosh.testsupport;

import org.assertj.core.api.AbstractAssert;

import hosh.spi.ExitStatus;

public class ExitStatusAssert extends AbstractAssert<ExitStatusAssert, ExitStatus> {

	public static ExitStatusAssert assertThat(ExitStatus actual) {
		return new ExitStatusAssert(actual);
	}

	private ExitStatusAssert(ExitStatus actual) {
		super(actual, ExitStatusAssert.class);
	}

	public void isSuccess() {
		isNotNull().matches(ExitStatus::isSuccess, "isSuccess()");
	}

	public void isError() {
		isNotNull()
				.matches(ExitStatus::isError, "isError()");
	}

	public void hasExitCode(int expectedCode) {
		isNotNull()
				.matches(exitStatus -> exitStatus.value() == expectedCode, "expecting exitCode " + expectedCode);
	}
}