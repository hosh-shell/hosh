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
package hosh.runtime;

import org.jline.reader.Candidate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatesTest {

	@Test
	void incomplete() {
		Candidate incomplete = Candidates.incomplete("file");
		assertThat(incomplete.value()).isEqualTo("file");
		assertThat(incomplete.complete()).isFalse();
		assertThat(incomplete.descr()).isNull();
	}

	@Test
	void complete() {
		Candidate complete = Candidates.complete("file");
		assertThat(complete.value()).isEqualTo("file");
		assertThat(complete.complete()).isTrue();
		assertThat(complete.descr()).isNull();
	}

	@Test
	void completeWithDescription() {
		Candidate complete = Candidates.completeWithDescription("file", "external command");
		assertThat(complete.value()).isEqualTo("file");
		assertThat(complete.complete()).isTrue();
		assertThat(complete.descr()).isEqualTo("external command");
	}
}
