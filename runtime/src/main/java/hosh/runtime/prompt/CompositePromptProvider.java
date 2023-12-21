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
package hosh.runtime.prompt;

import hosh.spi.State;

import java.util.List;
import java.util.StringJoiner;

// first attempt to enable composition of prompt, most likely it should be user-configurable
public class CompositePromptProvider implements PromptProvider {

	private final List<PromptProvider> providers;

	public CompositePromptProvider(List<PromptProvider> providers) {
		this.providers = providers;
	}

	@Override
	public String provide(State state) {
		StringJoiner result = new StringJoiner("");
		for (PromptProvider promptProvider: providers) {
			String prompt = promptProvider.provide(state);
			if (prompt != null) {
				result.add(prompt);
			}
		}
		if (result.length() == 0) {
			return null; // to respect the contract (null is possible)
		} else {
			return result.toString();
		}
	}
}
