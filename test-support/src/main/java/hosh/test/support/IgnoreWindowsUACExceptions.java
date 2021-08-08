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
package hosh.test.support;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * Special exception handler to ignore UAC exceptions on Windows.
 *
 * This is a workaround for local builds on Windows: by now on GitHub this
 * privilege is always held.
 */
public class IgnoreWindowsUACExceptions implements TestExecutionExceptionHandler {

	private final Logger logger = Logger.getLogger(IgnoreWindowsUACExceptions.class.getCanonicalName());

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		if (isUAC(throwable) || isUAC(throwable.getCause())) {
			logger.log(Level.WARNING, "ignoring UAC exception, please check", throwable);
			return;
		}
		throw throwable;
	}

	private boolean isUAC(Throwable throwable) {
		if (throwable == null) {
			return false;
		}
		String message = throwable.getMessage();
		if (message == null) {
			return false;
		}
		return message.contains("A required privilege is not held by the client.");
	}
}
