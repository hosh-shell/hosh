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
package hosh.spi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InputChannelTest {

	@Mock(stubOnly = true)
	InputChannel in;

	@Mock(stubOnly = true)
	Record record;

	@Test
	void empty() {
		given(in.recv()).willReturn(Optional.empty());
		Iterable<Record> iterable = InputChannel.iterate(in);
		assertThat(iterable).isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	void oneRecord() {
		given(in.recv()).willReturn(Optional.of(record), Optional.empty());
		Iterable<Record> iterable = InputChannel.iterate(in);
		assertThat(iterable).containsExactly(record);
	}

	@SuppressWarnings("unchecked")
	@Test
	void twoRecords() {
		given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
		Iterable<Record> iterable = InputChannel.iterate(in);
		assertThat(iterable).containsExactly(record, record);
	}

	@Test
	void throwsNoSuchElementsWhenConsumed() {
		given(in.recv()).willReturn(Optional.empty());
		Iterable<Record> iterable = InputChannel.iterate(in);
		Iterator<Record> iterator = iterable.iterator();
		assertThatThrownBy(iterator::next)
			.isInstanceOf(NoSuchElementException.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	void iterationTerminatesOnFirstEmptyResult() {
		given(in.recv()).willReturn(Optional.empty(), Optional.of(record));
		Iterable<Record> iterable = InputChannel.iterate(in);
		assertThat(iterable).isEmpty();
	}
}
