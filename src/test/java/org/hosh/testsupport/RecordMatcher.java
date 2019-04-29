/**
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
package org.hosh.testsupport;

import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;

import java.util.List;

import org.hosh.spi.Key;
import org.hosh.spi.Record;
import org.hosh.spi.Value;
import org.mockito.ArgumentMatcher;

/**
 * Mockito's argument matcher for Record that allows to specify
 * a subset of key/value mappings.
 * 
 * This has been done to allows unit tests with files e.g. in continuous
 * integration is it possible to check for a file named XXX but not the owner of
 * the file.
 */
public class RecordMatcher implements ArgumentMatcher<Record> {

	public static Record of(Key k1, Value v1) {
		RecordMatcher matcher = new RecordMatcher(List.of(new Record.Entry(k1, v1)));
		mockingProgress().getArgumentMatcherStorage().reportMatcher(matcher);
		return null;
	}

	public static Record of(Key k1, Value v1, Key k2, Value v2) {
		RecordMatcher matcher = new RecordMatcher(List.of(new Record.Entry(k1, v1), new Record.Entry(k2, v2)));
		mockingProgress().getArgumentMatcherStorage().reportMatcher(matcher);
		return null;
	}

	private final List<Record.Entry> entries;

	private RecordMatcher(List<Record.Entry> entries) {
		this.entries = entries;
	}

	@Override
	public boolean matches(Record argument) {
		if (argument == null) {
			return false;
		}
		for (var entry : entries) {
			if (!argument.entries().contains(entry)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "wanting entries: " + entries;
	}
}
