/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
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

import hosh.spi.Keys;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecordMatcherTest {

	@Test
	void whenRecordIsNull() {
		// Given
		RecordMatcher sut = new RecordMatcher(List.of(new Record.Entry(Keys.NAME, Values.ofText("mario"))));

		// When
		boolean result = sut.matches(null);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	void whenRecordIsEmpty() {
		// Given
		RecordMatcher sut = new RecordMatcher(List.of(new Record.Entry(Keys.NAME, Values.ofText("mario"))));

		// When
		boolean result = sut.matches(Records.empty());

		// Then
		assertThat(result).isFalse();
	}

	@Test
	void whenRecordContainsRequiredEntry() {
		// Given
		RecordMatcher sut = new RecordMatcher(List.of(new Record.Entry(Keys.NAME, Values.ofText("mario"))));
		Record mario = Records.singleton(Keys.NAME, Values.ofText("mario"));

		// When
		boolean result = sut.matches(mario);

		// Then
		assertThat(result).isTrue();
	}

	@Test
	void whenRecordContainsRequiredEntryPlusExtras() {
		// Given
		RecordMatcher sut = new RecordMatcher(List.of(new Record.Entry(Keys.NAME, Values.ofText("mario"))));
		Record marioAndCount = Records.singleton(Keys.NAME, Values.ofText("mario")).append(Keys.COUNT, Values.ofNumeric(42));

		// When
		boolean result = sut.matches(marioAndCount);

		// Then
		assertThat(result).isTrue();
	}

	@Test
	void whenRequiredEntryHasDifferentValue() {
		// Given
		RecordMatcher sut = new RecordMatcher(List.of(new Record.Entry(Keys.NAME, Values.ofText("mario"))));
		Record luigi = Records.singleton(Keys.NAME, Values.ofText("luigi"));

		// When
		boolean result = sut.matches(luigi);

		// Then
		assertThat(result).isFalse();
	}
}
