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
package hosh.spi.test.support;

import hosh.spi.Key;
import hosh.spi.Record;
import hosh.spi.Value;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.List;

/**
 * Mockito's argument matcher for Record that allows to specify
 * a subset of key/value mappings.
 * <p>
 * This has been done to allows unit tests with files in CI,
 * i.e. is it possible to check for a file named "foo.txt" but not the owner of the file.
 */
public class RecordMatcher implements ArgumentMatcher<Record> {

	public static Record of(Key k1, Value v1) {
		return Mockito.argThat(new RecordMatcher(List.of(new Record.Entry(k1, v1))));
	}

	public static Record of(Key k1, Value v1, Key k2, Value v2) {
		return Mockito.argThat(new RecordMatcher(List.of(new Record.Entry(k1, v1), new Record.Entry(k2, v2))));
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
