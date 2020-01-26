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
package hosh.runtime;

import org.jline.reader.History;
import org.jline.reader.LineReader;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ListIterator;

/**
 * Fake implementation of {@link History}: used for non-interactive sessions.
 */
public class DisabledHistory implements History {

	private UnsupportedOperationException unsupportedOperation() {
		return new UnsupportedOperationException("history is disabled");
	}

	@Override
	public void attach(LineReader reader) {
		throw unsupportedOperation();
	}

	@Override
	public void load() {
		throw unsupportedOperation();
	}

	@Override
	public void save() {
		throw unsupportedOperation();
	}

	@Override
	public void write(Path file, boolean incremental) {
		throw unsupportedOperation();
	}

	@Override
	public void append(Path file, boolean incremental) {
		throw unsupportedOperation();
	}

	@Override
	public void read(Path file, boolean incremental) {
		throw unsupportedOperation();
	}

	@Override
	public void purge() {
		throw unsupportedOperation();
	}

	@Override
	public int size() {
		throw unsupportedOperation();
	}

	@Override
	public int index() {
		throw unsupportedOperation();
	}

	@Override
	public int first() {
		throw unsupportedOperation();
	}

	@Override
	public int last() {
		throw unsupportedOperation();
	}

	@Override
	public String get(int index) {
		throw unsupportedOperation();
	}

	@Override
	public void add(Instant time, String line) {
		throw unsupportedOperation();
	}

	@Override
	public ListIterator<Entry> iterator(int index) {
		throw unsupportedOperation();
	}

	@Override
	public String current() {
		throw unsupportedOperation();
	}

	@Override
	public boolean previous() {
		throw unsupportedOperation();
	}

	@Override
	public boolean next() {
		throw unsupportedOperation();
	}

	@Override
	public boolean moveToFirst() {
		throw unsupportedOperation();
	}

	@Override
	public boolean moveToLast() {
		throw unsupportedOperation();
	}

	@Override
	public boolean moveTo(int index) {
		throw unsupportedOperation();
	}

	@Override
	public void moveToEnd() {
		throw unsupportedOperation();
	}

	@Override
	public void resetIndex() {
		throw unsupportedOperation();
	}
}
