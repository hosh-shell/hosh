/*
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
package hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import hosh.runtime.CommandResolvers.WindowsCommandResolver;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.OutputChannel;
import hosh.spi.State;
import hosh.testsupport.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

public class CommandResolversTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class BuiltinsThenSystemTest {

		@RegisterExtension
		public final TemporaryFolder folder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private Command command;

		@Mock(stubOnly = true)
		private State state;

		private CommandResolver sut;

		@BeforeEach
		public void setup() {
			sut = CommandResolvers.builtinsThenExternal(state);
		}

		@Test
		public void notFound() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Collections.emptyList());
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		public void builtin() {
			given(state.getCommands()).willReturn(Map.of("test", command.getClass()));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent();
		}

		@Test
		public void validAbsolutePath() throws IOException {
			File file = folder.newFile("test");
			assert file.setExecutable(true);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			Optional<Command> result = sut.tryResolve(file.getAbsolutePath());
			assertThat(result).isPresent();
		}

		@Test
		public void invalidAbsolutePath() throws IOException {
			File file = folder.newFile("test");
			given(state.getCommands()).willReturn(Collections.emptyMap());
			Optional<Command> result = sut.tryResolve(file.getAbsolutePath() + "_invalid");
			assertThat(result).isEmpty();
		}

		@Test
		public void invalidSkipDirectory() throws IOException {
			folder.newFolder("test");
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getCommands()).willReturn(Collections.emptyMap());
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		@DisabledOnOs(OS.WINDOWS) // in Windows this file is marked as executable!?
		public void foundNonExecutableInPath() throws IOException {
			assert folder.newFile("test").setExecutable(false);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isNotPresent();
		}

		@Test
		public void foundExecutableInPath() throws IOException {
			assert folder.newFile("test").setExecutable(true);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent();
		}

		@Test
		@EnabledOnOs(OS.WINDOWS)
		public void notFoundInPathAsSpecifiedByPathExt() throws IOException {
			assert folder.newFile("test.vbs").setExecutable(true); // VBS in not PATHEXT
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getVariables()).willReturn(Map.of("PATHEXT", ".COM;.EXE;.BAT;.CMD"));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isNotPresent();
		}

		@Test
		@EnabledOnOs(OS.WINDOWS)
		public void foundInPathAsSpecifiedByPathExt() throws IOException {
			assert folder.newFile("test.exe").setExecutable(true);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getVariables()).willReturn(Map.of("PATHEXT", ".COM;.EXE;.BAT;.CMD"));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent();
		}

		@Test
		public void foundInCwd() throws IOException {
			assert folder.newFile("test").setExecutable(true);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("./test");
			assertThat(result).isPresent();
		}

		@Test
		public void notFoundInPath() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		public void builtinClassWithoutNoArgsConstructor() {
			given(state.getCommands()).willReturn(Map.of("test", InvalidCommand.class));
			assertThatThrownBy(() -> sut.tryResolve("test"))
					.hasMessageStartingWith("cannot create instance of class")
					.isInstanceOf(IllegalArgumentException.class);
		}

		private class InvalidCommand implements Command {

			@SuppressWarnings("unused")
			public InvalidCommand(String arg) {
			}

			@Override
			public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
				return ExitStatus.success();
			}
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class WindowsCommandResolverTest {

		@RegisterExtension
		public final TemporaryFolder folder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private Command command;

		@Mock(stubOnly = true)
		private State state;

		private WindowsCommandResolver sut;

		@BeforeEach
		public void setup() {
			sut = new WindowsCommandResolver(state);
		}

		@Test
		public void pathExtNotDefined() {
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		public void findExecutableInPathext() throws IOException {
			assert folder.newFile("TEST.EXE").setExecutable(true);
			given(state.getVariables()).willReturn(Map.of("PATHEXT", ".COM;.EXE"));
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("TEST");
			assertThat(result).isNotEmpty();
		}

		@Test
		public void findExecutableNotInPathext() throws IOException {
			assert folder.newFile("TEST.CMD").setExecutable(true);
			given(state.getVariables()).willReturn(Map.of("PATHEXT", ".COM;.EXE"));
			given(state.getPath()).willReturn(List.of(folder.toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("TEST");
			assertThat(result).isEmpty();
		}
	}
}
