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

import hosh.spi.Command;
import hosh.spi.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class SimpleCommandRegistryTest {

	@Mock(stubOnly = true)
	private Command command;

	@Mock(stubOnly = true)
	private Command anotherCommand;

	@Spy
	private final State state = new State();

	@InjectMocks
	private SimpleCommandRegistry sut;

	@Test
	public void oneCommand() {
		sut.registerCommand("foo", () -> command);
		assertThat(state.getCommands()).containsKey("foo");
	}

	@Test
	public void oneCommandWithAlias() {
		sut.registerCommand("foo", () -> command);
		sut.registerCommand("bar", () -> command);
		assertThat(state.getCommands()).containsKeys("foo", "bar");
	}

	@Test
	public void twoDifferentCommands() {
		sut.registerCommand("foo", () -> command);
		sut.registerCommand("bar", () -> anotherCommand);
		assertThat(state.getCommands()).containsKeys("foo", "bar");
	}

	@Test
	public void twoTimesSameCommand() {
		Supplier<Command> supplier = () -> command;
		assertThatThrownBy(() -> {
			sut.registerCommand("foo", supplier);
			sut.registerCommand("foo", () -> anotherCommand);
		}).hasMessage("command with same name already registered: foo")
			.isInstanceOf(IllegalArgumentException.class);
		// make sure first mapping is still in place
		assertThat(state.getCommands()).containsEntry("foo", supplier);
	}
}
