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

import hosh.runtime.CommandResolvers.WindowsCommandResolver;
import hosh.spi.Command;
import hosh.spi.State;
import hosh.spi.VariableName;
import hosh.test.support.TemporaryFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

class CommandResolversTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class BuiltinsThenSystemTest {

		@RegisterExtension
		final TemporaryFolder folder = new TemporaryFolder();

		@Mock(stubOnly = true)
		Command command;

		@Mock(stubOnly = true)
		State state;

		CommandResolver sut;

		@BeforeEach
		void setup() {
			sut = CommandResolvers.builtinsThenExternal(state);
		}

		@Test
		void notFound() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Collections.emptyList());
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		void builtin() {
			given(state.getCommands()).willReturn(Map.of("test", () -> command));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent().hasValue(command);
		}

		@Test
		void validAbsolutePath() throws IOException {
			File file = folder.newFile("test");
			assertThat(file.setExecutable(true)).isTrue();
			given(state.getCommands()).willReturn(Collections.emptyMap());
			Optional<Command> result = sut.tryResolve(file.getAbsolutePath());
			assertThat(result).isPresent();
		}

		@Test
		void invalidAbsolutePath() throws IOException {
			File file = folder.newFile("test");
			given(state.getCommands()).willReturn(Collections.emptyMap());
			Optional<Command> result = sut.tryResolve(file.getAbsolutePath() + "_invalid");
			assertThat(result).isEmpty();
		}

		@Test
		void invalidDirectory() throws IOException {
			folder.newFolder("test");
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getCommands()).willReturn(Collections.emptyMap());
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@DisabledOnOs(OS.WINDOWS)
		@Test
		void foundNonExecutableInPath() throws IOException {
			assertThat(folder.newFile("test").setExecutable(false)).isTrue();
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isNotPresent();
		}

		@Test
		void foundExecutableInPath() throws IOException {
			assertThat(folder.newFile("test").setExecutable(true)).isTrue();
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent();
		}
		@Test
		@EnabledOnOs(OS.WINDOWS)
		void notFoundInPathAsSpecifiedByPathExt() throws IOException {
			assertThat(folder.newFile("test.vbs").setExecutable(true)).isTrue(); // VBS in not PATHEXT
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getVariables()).willReturn(Map.of(VariableName.constant("PATHEXT"), ".COM;.EXE;.BAT;.CMD"));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isNotPresent();
		}

		@Test
		@EnabledOnOs(OS.WINDOWS)
		void foundInPathAsSpecifiedByPathExt() throws IOException {
			assertThat(folder.newFile("test.exe").setExecutable(true)).isTrue();
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getVariables()).willReturn(Map.of(VariableName.constant("PATHEXT"), ".COM;.EXE;.BAT;.CMD"));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent();
		}

		@Test
		void foundInCwd() throws IOException {
			assertThat(folder.newFile("test").setExecutable(true)).isTrue();
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("./test");
			assertThat(result).isPresent();
		}

		@Test
		void notFoundInPath() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class WindowsCommandResolverTest {

		@RegisterExtension
		final TemporaryFolder folder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		WindowsCommandResolver sut;

		@BeforeEach
		void setup() {
			sut = new WindowsCommandResolver(state);
		}

		@Test
		void pathExtNotDefined() {
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		void findExecutableInPathext() throws IOException {
			assertThat(folder.newFile("TEST.EXE").setExecutable(true)).isTrue();
			given(state.getVariables()).willReturn(Map.of(VariableName.constant("PATHEXT"), ".COM;.EXE"));
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("TEST");
			assertThat(result).isNotEmpty();
		}

		@Test
		void findExecutableNotInPathext() throws IOException {
			assertThat(folder.newFile("TEST.CMD").setExecutable(true)).isTrue();
			given(state.getVariables()).willReturn(Map.of(VariableName.constant("PATHEXT"), ".COM;.EXE"));
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("TEST");
			assertThat(result).isEmpty();
		}
	}
}
