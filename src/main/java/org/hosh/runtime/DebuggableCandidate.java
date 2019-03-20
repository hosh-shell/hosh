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
package org.hosh.runtime;

import java.util.Objects;

import org.jline.reader.Candidate;

/**
 * {@link Candidate} as value object
 */
public class DebuggableCandidate extends Candidate {

	public static DebuggableCandidate incomplete(String value) {
		return new DebuggableCandidate(value, false);
	}

	public static DebuggableCandidate complete(String value) {
		return new DebuggableCandidate(value, true);
	}

	public static DebuggableCandidate completeWithDescription(String value, String description) {
		return new DebuggableCandidate(value, description, true);
	}

	private DebuggableCandidate(String value, String desc, boolean complete) {
		super(value, value, null, desc, null, null, complete);
	}

	private DebuggableCandidate(String value, boolean complete) {
		this(value, null, complete);
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof DebuggableCandidate) {
			DebuggableCandidate that = (DebuggableCandidate) obj;
			return Objects.equals(this.value(), that.value());
		} else {
			return false;
		}
	}

	@Override
	public final int hashCode() {
		return Objects.hash(this.value());
	}

	@Override
	public String toString() {
		return String.format("Candidate[value='%s',complete=%s]", value(), complete());
	}
}
