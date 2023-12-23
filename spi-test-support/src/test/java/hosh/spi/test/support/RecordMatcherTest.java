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
	void usage() {
		RecordMatcher sut = new RecordMatcher(List.of(new Record.Entry(Keys.NAME, Values.ofText("mario"))));

		Record empty = Records.empty();
		assertThat(sut.matches(empty)).isFalse();

		Record mario = empty.append(Keys.NAME, Values.ofText("mario"));
		assertThat(sut.matches(mario)).isTrue();

		Record marioAndCount = mario.append(Keys.COUNT, Values.ofNumeric(42));
		assertThat(sut.matches(marioAndCount)).isTrue(); // still true, as the required key/value is still there

		Record luigi = empty.append(Keys.NAME, Values.ofText("luigi"));
		assertThat(sut.matches(luigi)).isFalse(); // not matching the required key/value (mario)
	}
}