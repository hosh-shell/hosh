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

import hosh.spi.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SimpleCommandRegistryTest {

	@Mock(stubOnly = true)
	Command command;

	@Mock(stubOnly = true)
	Command anotherCommand;

	SimpleCommandRegistry sut;

	@BeforeEach
	void createSut() {
		sut = new SimpleCommandRegistry();
	}

	@Test
	void oneCommand() {
		sut.registerCommand("foo", () -> command);
		assertThat(sut.getCommands()).containsKey("foo");
	}

	@Test
	void oneCommandWithAlias() {
		sut.registerCommand("foo", () -> command);
		sut.registerCommand("bar", () -> command);
		assertThat(sut.getCommands()).containsKeys("foo", "bar");
	}

	@Test
	void twoDifferentCommands() {
		sut.registerCommand("foo", () -> command);
		sut.registerCommand("bar", () -> anotherCommand);
		assertThat(sut.getCommands()).containsKeys("foo", "bar");
	}

	@Test
	void twoTimesSameCommand() {
		Supplier<Command> commandSupplier = () -> command;
		sut.registerCommand("foo", commandSupplier);
		assertThatThrownBy(() -> sut.registerCommand("foo", () -> anotherCommand)).hasMessage("command with same name already registered: foo")
			.isInstanceOf(IllegalArgumentException.class);
		// make sure first mapping is still in place
		assertThat(sut.getCommands()).containsEntry("foo", commandSupplier);
	}
}
