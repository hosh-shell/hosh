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
package hosh.modules.filesystem;

import hosh.doc.Bug;
import hosh.spi.Ansi;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.test.support.IgnoreWindowsUACExceptions;
import hosh.test.support.RecordMatcher;
import hosh.test.support.TemporaryFolder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static hosh.test.support.ExitStatusAssert.assertThat;

public class FileSystemModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	@ExtendWith(IgnoreWindowsUACExceptions.class)
	public class ListFilesTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.ListFiles sut;

		@Test
		public void errorTwoOrMoreArgs() {
			ExitStatus exitStatus = sut.run(List.of("dir1", "dir2"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: ls [directory]")));
		}

		@Test
		public void zeroArgsWithEmptyDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void zeroArgsWithOneDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
				RecordMatcher.of(
					Keys.PATH, Values.withStyle(Values.ofPath(Paths.get("dir")), Ansi.Style.FG_CYAN),
					Keys.SIZE, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void zeroArgsWithOneFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			temporaryFolder.newFile("file");
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
				Keys.PATH, Values.withStyle(Values.ofPath(Paths.get("file")), Ansi.Style.NONE),
				Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void zeroArgsWithOneSymlink() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("file").toPath();
			Files.createSymbolicLink(Paths.get(state.getCwd().toString(), "link"), file);
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
				Keys.PATH, Values.withStyle(Values.ofPath(Paths.get("file")), Ansi.Style.NONE),
				Keys.SIZE, Values.ofSize(0)));
			then(out).should().send(RecordMatcher.of(
				Keys.PATH, Values.withStyle(Values.ofPath(Paths.get("link")), Ansi.Style.FG_MAGENTA),
				Keys.SIZE, Values.none()));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneArgWithRelativeFile() throws IOException {
			Path file = temporaryFolder.newFile().toPath();
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(file.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory: " + file.toAbsolutePath().toString())));
		}

		@Test
		public void oneArgWithEmptyDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.newFolder().toPath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgWithNonEmptyDirectory() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(newFolder.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(ArgumentMatchers.any());
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgReferringToCwd() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(newFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
				Keys.PATH, Values.withStyle(Values.ofPath(Paths.get("aaa")), Ansi.Style.NONE),
				Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgAbsoluteFile() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Path path = Files.createFile(new File(newFolder, "aaa").toPath()).toAbsolutePath();
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(path.toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory: " + path)));
		}

		@Test
		public void oneArgAbsoluteDir() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(newFolder.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
				Keys.PATH, Values.withStyle(Values.ofPath(Paths.get("aaa")), Ansi.Style.NONE),
				Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void listDot() throws IOException {
			temporaryFolder.newFile("aaa");
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
				Keys.PATH, Values.withStyle(Values.ofPath(Paths.get("aaa")), Ansi.Style.NONE),
				Keys.SIZE, Values.ofSize(0)));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void listDotDot() throws IOException {
			File cwd = temporaryFolder.newFolder("aaa");
			given(state.getCwd()).willReturn(cwd.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(".."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
				RecordMatcher.of(
					Keys.PATH, Values.withStyle(Values.ofPath(Path.of("aaa")), Ansi.Style.FG_CYAN),
					Keys.SIZE, Values.none()));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@DisabledOnOs(OS.WINDOWS) // File.setReadable() fails on windows
		@Bug(description = "check handling of java.nio.file.AccessDeniedException", issue = "https://github.com/dfa1/hosh/issues/74")
		@Test
		public void accessDenied() {
			File cwd = temporaryFolder.toFile();
			assertThat(cwd.exists()).isTrue();
			assertThat(cwd.setReadable(false, true)).isTrue();
			assertThat(cwd.setExecutable(false, true)).isTrue();
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("access denied: " + cwd.toString())));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ChangeDirectoryTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.ChangeDirectory sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cd directory")));
		}

		@Test
		public void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("asd", "asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cd directory")));
		}

		@Test
		public void oneDirectoryRelativeArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFolder = temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of("dir"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(state).should().setCwd(newFolder.toPath().toAbsolutePath());
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneDirectoryAbsoluteArgument() throws IOException {
			File newFolder = temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of(newFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(state).should().setCwd(newFolder.toPath());
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneFileArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isError();
			then(state).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class CurrentWorkingDirectoryTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.CurrentWorkingDirectory sut;

		@Test
		public void noArgs() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(temporaryFolder.toPath())));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cwd")));
			then(out).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class LinesTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Lines sut;

		@Test
		public void emptyFile() throws IOException {
			File newFile = temporaryFolder.newFile("data.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void nonEmptyFile() throws IOException {
			File newFile = temporaryFolder.newFile("data.txt");
			try (FileWriter writer = new FileWriter(newFile, StandardCharsets.UTF_8)) {
				writer.write("a 1\n");
				writer.write("b 2\n");
			}
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("a 1")));
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("b 2")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void nonEmptyFileInCwd() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("data.txt");
			try (FileWriter writer = new FileWriter(newFile, StandardCharsets.UTF_8)) {
				writer.write("a 1\n");
			}
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("a 1")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void directory() {
			ExitStatus exitStatus = sut.run(List.of(temporaryFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not readable file")));
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: lines file")));
			then(out).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class CopyTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Copy sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cp file file")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("source.txt"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cp file file")));
		}

		@Test
		public void copyRelativeToRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File source = temporaryFolder.newFile("source.txt");
			File target = temporaryFolder.newFile("target.txt");
			assertThat(target.delete()).isTrue();
			ExitStatus exitStatus = sut.run(List.of(source.getName(), target.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(source).exists();
			assertThat(target).exists();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void copyAbsoluteToAbsolute() throws IOException {
			File source = temporaryFolder.newFile("source.txt").getAbsoluteFile();
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			assertThat(target.delete()).isTrue();
			ExitStatus exitStatus = sut.run(List.of(source.getAbsolutePath(), target.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(source).exists();
			assertThat(target).exists();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class MoveTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Move sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: mv file file")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("source.txt"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: mv file file")));
		}

		@Test
		public void moveRelativeToRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File source = temporaryFolder.newFile("source.txt");
			File target = temporaryFolder.newFile("target.txt");
			assertThat(target.delete()).isTrue();
			ExitStatus exitStatus = sut.run(List.of(source.getName(), target.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(source).doesNotExist();
			assertThat(target).exists();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void moveAbsoluteToAbsolute() throws IOException {
			File source = temporaryFolder.newFile("source.txt").getAbsoluteFile();
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			assertThat(target.delete()).isTrue();
			ExitStatus exitStatus = sut.run(List.of(source.getAbsolutePath(), target.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(source).doesNotExist();
			assertThat(target).exists();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class RemoveTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Remove sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: rm file")));
		}

		@Test
		public void removeRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			ExitStatus exitStatus = sut.run(List.of(target.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(target).doesNotExist();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void removeAbsolute() throws IOException {
			File target = temporaryFolder.newFile("target.txt").getAbsoluteFile();
			ExitStatus exitStatus = sut.run(List.of(target.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(target).doesNotExist();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class PartitionsTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Partitions sut;

		@Test
		public void listAllPartitions() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(Mockito.any());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void tooManyArgs() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: partitions")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	@ExtendWith(IgnoreWindowsUACExceptions.class)
	public class WalkTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Walk sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: walk directory")));
			then(out).shouldHaveNoInteractions();
		}

		@Test
		public void emptyRelativeDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void nonEmptyRelativeDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath()), Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void nonExistentRelativeDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("path"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not found")));
		}

		@Test
		public void nonEmptyAbsoluteDirectory() throws IOException {
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(temporaryFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath()), Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void absoluteFile() throws IOException {
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory")));
		}

		@Test
		public void resolveSymlinks() throws IOException {
			File newFolder = temporaryFolder.newFolder("folder");
			File newFile = temporaryFolder.newFile(newFolder, "file.txt");
			File symlink = new File(temporaryFolder.toFile(), "symlink");
			Files.createSymbolicLink(symlink.toPath(), newFolder.toPath());
			given(state.getCwd()).willReturn(temporaryFolder.toPath()); // previous method could fail by UAC and Mockito will throw UnnecessaryStubbingException
			ExitStatus exitStatus = sut.run(List.of("symlink"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(Keys.PATH, Values.ofPath(newFile.toPath()), Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ProbeTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Probe sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: probe file")));
		}

		@Test
		public void probeKnown() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.of("contenttype"), Values.ofText("text/plain")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void probeUnknown() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.ppp");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("content type cannot be determined")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class GlobTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Glob sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: glob pattern")));
		}

		@SuppressWarnings("unchecked")
		@Test
		public void matchRelativePath() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Record record = Records.singleton(Keys.PATH, Values.ofPath(Path.of("file.java")));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("*.java"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(times(2)).recv();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void matchAbsolutePath() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Record record = Records.singleton(Keys.PATH, Values.ofPath(Path.of("/tmp/file.java")));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("*.java"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(times(2)).recv();
			then(out).should().send(record);
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void noMatch() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Record record = Records.singleton(Keys.PATH, Values.ofPath(Path.of("file.java")));
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("*.c"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(times(2)).recv();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	@ExtendWith(IgnoreWindowsUACExceptions.class)
	public class SymlinkTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Symlink sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: symlink source target")));
		}

		@Test
		public void symlinkFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName(), "link"), in, out, err);
			Path link = temporaryFolder.toPath().resolve("link");
			assertThat(link).isSymbolicLink();
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class HardlinkTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Hardlink sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: hardlink source target")));
		}

		@Test
		public void symlinkFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getName(), "link"), in, out, err);
			Path link = temporaryFolder.toPath().resolve("link");
			assertThat(exitStatus).isSuccess();
			assertThat(link).exists();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	@ExtendWith(IgnoreWindowsUACExceptions.class)
	public class ResolveTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.Resolve sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: resolve file")));
		}

		@Test
		public void regularRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of("file.txt"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath().toRealPath())));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void regularAbsolute() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getAbsolutePath()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath().toRealPath())));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void symlink() throws IOException {
			File newFile = temporaryFolder.newFile("file.txt");
			Files.createSymbolicLink(Path.of(temporaryFolder.toFile().getAbsolutePath(), "link"), newFile.toPath());
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("link"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toPath().toAbsolutePath().toRealPath())));
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class WithLockTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private FileSystemModule.WithLock sut;

		@Test
		public void noArgs() {
			assertThatThrownBy(() -> sut.before(List.of(), in, out, err))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("expecting file name");
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void wrongFile() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			assertThatThrownBy(() -> sut.before(List.of("../missing_directory/file.txt"), in, out, err))
				.isInstanceOf(UncheckedIOException.class);
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void lock() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			File lockFile = temporaryFolder.newFile("file.txt");
			FileSystemModule.WithLock.LockResource resource = sut.before(List.of("file.txt"), in, out, err);
			assertThat(resource).isNotNull();
			// under same JVM tryLock throws exception
			assertThatThrownBy(() -> resource.getRandomAccessFile().getChannel().tryLock())
				.isInstanceOf(OverlappingFileLockException.class);
			sut.after(resource, in, out, err);
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(lockFile).doesNotExist();
		}
	}
}
