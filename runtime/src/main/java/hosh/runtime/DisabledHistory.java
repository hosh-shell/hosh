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
import java.util.Collections;
import java.util.ListIterator;

/**
 * Fake no-op implementation of {@link History}: used for non-interactive sessions and for HoshIT.
 */
public class DisabledHistory implements History {

	@Override
	public void attach(LineReader reader) {
	}

	@Override
	public void load() {
	}

	@Override
	public void save() {
	}

	@Override
	public void write(Path file, boolean incremental) {
	}

	@Override
	public void append(Path file, boolean incremental) {
	}

	@Override
	public void read(Path file, boolean incremental) {
	}

	@Override
	public void purge() {
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public int index() {
		return 0;
	}

	@Override
	public int first() {
		return 0;
	}

	@Override
	public int last() {
		return 0;
	}

	@Override
	public String get(int index) {
		return null;
	}

	@Override
	public void add(Instant time, String line) {
	}

	@Override
	public ListIterator<Entry> iterator(int index) {
		return Collections.emptyListIterator();
	}

	@Override
	public String current() {
		return null;
	}

	@Override
	public boolean previous() {
		return false;
	}

	@Override
	public boolean next() {
		return false;
	}

	@Override
	public boolean moveToFirst() {
		return false;
	}

	@Override
	public boolean moveToLast() {
		return false;
	}

	@Override
	public boolean moveTo(int index) {
		return false;
	}

	@Override
	public void moveToEnd() {
	}

	@Override
	public void resetIndex() {
	}
}
