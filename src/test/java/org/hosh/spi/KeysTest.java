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
package org.hosh.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hosh.spi.Keys.SingleWordKey;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class KeysTest {

	@Test
	public void nullKey() {
		assertThatThrownBy(() -> Keys.of(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("name must be not null");
	}

	@Test
	public void emptyKey() {
		assertThatThrownBy(() -> Keys.of(""))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("name must be a single lowercase word");
	}

	@Test
	public void uppercaseKey() {
		assertThatThrownBy(() -> Keys.of("AAA"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("name must be a single lowercase word");
	}

	@Test
	public void twoWordsKey() {
		assertThatThrownBy(() -> Keys.of("aaa bbb"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("name must be a single lowercase word");
	}

	@Test
	public void representation() {
		assertThat(Keys.of("name"))
				.hasToString("Key[name]");
	}

	@Test
	public void equalContract() {
		EqualsVerifier.configure()
				.forClass(SingleWordKey.class)
				.verify();
	}
}
