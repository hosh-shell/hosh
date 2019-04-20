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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SimpleCommandRegistryTest {

	@Mock(stubOnly = true)
	private Command command;

	@Mock(stubOnly = true)
	private Command anotherCommand;

	@Spy
	private State state = new State();

	@InjectMocks
	private SimpleCommandRegistry sut;

	@Test
	public void oneCommand() {
		sut.registerCommand("foo", command.getClass());
		assertThat(state.getCommands())
				.containsEntry("foo", command.getClass());
	}

	@Test
	public void sameCommandTwice() {
		sut.registerCommand("foo", command.getClass());
		sut.registerCommand("bar", command.getClass());
		assertThat(state.getCommands())
				.containsEntry("foo", command.getClass())
				.containsEntry("bar", command.getClass());
	}

	@Test
	public void twoDifferentCommands() {
		sut.registerCommand("foo", command.getClass());
		sut.registerCommand("bar", anotherCommand.getClass());
		assertThat(state.getCommands())
				.containsEntry("foo", command.getClass())
				.containsEntry("bar", anotherCommand.getClass());
	}

	@Test
	public void twoTimesSameCommand() {
		assertThatThrownBy(() -> {
			sut.registerCommand("foo", command.getClass());
			sut.registerCommand("foo", anotherCommand.getClass());
		}).hasMessage("command with same name already registered: foo")
				.isInstanceOf(IllegalArgumentException.class);
		// make sure first mapping is still in place
		assertThat(state.getCommands()).containsEntry("foo", command.getClass());
	}
}
