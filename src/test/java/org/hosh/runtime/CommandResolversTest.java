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
import static org.mockito.BDDMockito.given;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hosh.runtime.CommandResolvers.WindowsCommandResolver;
import org.hosh.runtime.CommandResolversTest.BuiltinsThenSystemTest;
import org.hosh.runtime.CommandResolversTest.WindowsCommandResolverTest;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.State;
import org.hosh.testsupport.IgnoreIf;
import org.hosh.testsupport.IgnoreIf.IgnoredIf;
import org.hosh.testsupport.IgnoreIf.NotOnWindows;
import org.hosh.testsupport.IgnoreIf.OnWindows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({
		BuiltinsThenSystemTest.class,
		WindowsCommandResolverTest.class,
})
public class CommandResolversTest {

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class BuiltinsThenSystemTest {

		@Rule
		public final TemporaryFolder folder = new TemporaryFolder();

		@Rule
		public final IgnoreIf ignoreIf = new IgnoreIf();

		@Mock(stubOnly = true)
		private Command command;

		@Mock(stubOnly = true)
		private State state;

		private CommandResolver sut;

		@Before
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
			given(state.getCommands()).willReturn(Collections.singletonMap("test", command.getClass()));
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
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getCommands()).willReturn(Collections.emptyMap());
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		@IgnoredIf(description = "in Windows this file is marked as executable, why?", condition = OnWindows.class)
		public void foundNonExecutableInPath() throws IOException {
			assert folder.newFile("test").setExecutable(false);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isNotPresent();
		}

		@Test
		public void foundExecutableInPath() throws IOException {
			assert folder.newFile("test").setExecutable(true);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent();
		}

		@Test
		@IgnoredIf(description = "valid only in Windows", condition = NotOnWindows.class)
		public void notFoundInPathAsSpecifiedByPathExt() throws IOException {
			assert folder.newFile("test.vbs").setExecutable(true); // VBS in not PATHEXT
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getVariables()).willReturn(Collections.singletonMap("PATHEXT", ".COM;.EXE;.BAT;.CMD"));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isNotPresent();
		}

		@Test
		@IgnoredIf(description = "valid only in Windows", condition = NotOnWindows.class)
		public void foundInPathAsSpecifiedByPathExt() throws IOException {
			assert folder.newFile("test.exe").setExecutable(true);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			given(state.getVariables()).willReturn(Collections.singletonMap("PATHEXT", ".COM;.EXE;.BAT;.CMD"));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isPresent();
		}

		@Test
		public void foundInCwd() throws IOException {
			assert folder.newFile("test").setExecutable(true);
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("./test");
			assertThat(result).isPresent();
		}

		@Test
		public void notFoundInPath() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		public void builtinClassWithoutNoArgsConstructor() {
			given(state.getCommands()).willReturn(Collections.singletonMap("test", InvalidCommand.class));
			assertThatThrownBy(() -> sut.tryResolve("test"))
					.hasMessageStartingWith("cannot create instance of class")
					.isInstanceOf(IllegalArgumentException.class);
		}

		private static class InvalidCommand implements Command {

			@SuppressWarnings("unused")
			public InvalidCommand(String arg) {
			}

			@Override
			public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
				return ExitStatus.success();
			}
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class WindowsCommandResolverTest {

		@Rule
		public final TemporaryFolder folder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private Command command;

		@Mock(stubOnly = true)
		private State state;

		private WindowsCommandResolver sut;

		@Before
		public void setup() {
			sut = new WindowsCommandResolver(state);
		}

		@Test
		public void pathExtNotDefined() {
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("test");
			assertThat(result).isEmpty();
		}

		@Test
		public void findExecutableInPathext() throws IOException {
			assert folder.newFile("TEST.EXE").setExecutable(true);
			given(state.getVariables()).willReturn(Map.of("PATHEXT", ".COM;.EXE"));
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("TEST");
			assertThat(result).isNotEmpty();
		}

		@Test
		public void findExecutableNotInPathext() throws IOException {
			assert folder.newFile("TEST.CMD").setExecutable(true);
			given(state.getVariables()).willReturn(Map.of("PATHEXT", ".COM;.EXE"));
			given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
			given(state.getCwd()).willReturn(Paths.get("."));
			Optional<Command> result = sut.tryResolve("TEST");
			assertThat(result).isEmpty();
		}
	}
}
