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
package hosh.testsupport;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * To be used to control current thread name or interrupted flag.
 */
public class WithThread implements Extension, BeforeTestExecutionCallback, AfterTestExecutionCallback {

	private String backup;

	@Override
	public void afterTestExecution(ExtensionContext context) {
		Thread.currentThread().setName(backup);
		Thread.interrupted();
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) {
		backup = Thread.currentThread().getName();
	}

	public String currentName() {
		return Thread.currentThread().getName();
	}

	public void renameTo(String newName) {
		Thread.currentThread().setName(newName);
	}

	public boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}

	public void interrupt() {
		Thread.currentThread().interrupt();
	}

}
