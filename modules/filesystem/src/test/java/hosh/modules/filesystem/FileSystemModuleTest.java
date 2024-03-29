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
package hosh.modules.filesystem;

import hosh.doc.Bug;
import hosh.spi.CommandWrapper;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateMutator;
import hosh.spi.Values;
import hosh.spi.test.support.RecordMatcher;
import hosh.test.support.TemporaryFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

class FileSystemModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ListFilesTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.ListFiles sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.ListFiles();
			sut.setState(state);
		}

		@Test
		void errorTwoOrMoreArgs() {
			ExitStatus exitStatus = sut.run(List.of("dir1", "dir2"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: ls [directory]")));
		}

		@Test
		void zeroArgsWithEmptyDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void zeroArgsWithOneDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
					RecordMatcher.of(
							Keys.PATH, Values.ofPath(Paths.get("dir")),
							Keys.SIZE, Values.none()));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void zeroArgsWithOneFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			temporaryFolder.newFile("file");
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("file")),
					Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void zeroArgsWithOneSymlink() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("file");
			Files.createSymbolicLink(Paths.get(state.getCwd().toString(), "link"), file);
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("file")),
					Keys.SIZE, Values.ofSize(0)));
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("link")),
					Keys.SIZE, Values.none()));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArgWithRelativeFile() throws IOException {
			Path file = temporaryFolder.newFile(temporaryFolder.toPath(), "aaa");
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(file.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory: " + file.toAbsolutePath())));
		}

		@Test
		void oneArgWithEmptyDirectory() {
			Path cwd = temporaryFolder.toPath();
			given(state.getCwd()).willReturn(cwd);
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArgWithNonEmptyDirectory() throws IOException {
			Path cwd = temporaryFolder.toPath();
			given(state.getCwd()).willReturn(cwd);
			Files.createFile(cwd.resolve("aaa"));
			ExitStatus exitStatus = sut.run(List.of(cwd.toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(any());
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArgReferringToCwd() throws IOException {
			Path cwd = temporaryFolder.toPath();
			Files.createFile(cwd.resolve("aaa"));
			given(state.getCwd()).willReturn(cwd);
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("aaa")),
					Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void oneArgAbsoluteFile() throws IOException {
			Path cwd = temporaryFolder.toPath();
			Path file = Files.createFile(cwd.resolve("aaa"));
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(file.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory: " + file)));
		}

		@Test
		void oneArgAbsoluteDir() throws IOException {
			Path cwd = temporaryFolder.toPath();
			Files.createFile(cwd.resolve("aaa"));
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(cwd.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("aaa")),
					Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void listDot() throws IOException {
			temporaryFolder.newFile("aaa");
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(
					Keys.PATH, Values.ofPath(Paths.get("aaa")),
					Keys.SIZE, Values.ofSize(0)));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		void listDotDot() throws IOException {
			Path cwd = temporaryFolder.newFolder("aaa");
			given(state.getCwd()).willReturn(cwd.toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(".."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
					RecordMatcher.of(
							Keys.PATH, Values.ofPath(Path.of("aaa")),
							Keys.SIZE, Values.none()));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoMoreInteractions();
		}

		@Bug(description = "check handling of java.nio.file.AccessDeniedException", issue = "https://github.com/dfa1/hosh/issues/74")
		@Test
		@DisabledOnOs(value = {
				OS.WINDOWS  // File.setReadable() fails on Windows
		})
		void accessDenied() {
			File cwd = temporaryFolder.toPath().toFile();
			assertThat(cwd).exists();
			assertThat(cwd.setReadable(false)).isTrue();
			assertThat(cwd.setExecutable(false)).isTrue();
			given(state.getCwd()).willReturn(temporaryFolder.toPath().toAbsolutePath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("access denied: " + cwd)));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ChangeDirectoryTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		StateMutator stateMutator;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.ChangeDirectory sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.ChangeDirectory();
			sut.setState(state);
			sut.setStateMutator(stateMutator);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cd directory")));
		}

		@Test
		void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("asd", "asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cd directory")));
		}

		@Test
		void oneDirectoryRelativeArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFolder = temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of("dir"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(stateMutator).should().mutateCwd(newFolder.toAbsolutePath());
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneDirectoryAbsoluteArgument() throws IOException {
			Path newFolder = temporaryFolder.newFolder("dir");
			ExitStatus exitStatus = sut.run(List.of(newFolder.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(stateMutator).should().mutateCwd(newFolder);
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneFileArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file");
			ExitStatus exitStatus = sut.run(List.of(newFile.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(stateMutator).shouldHaveNoMoreInteractions();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class CurrentWorkingDirectoryTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.CurrentWorkingDirectory sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.CurrentWorkingDirectory();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(temporaryFolder.toPath())));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cwd")));
			then(out).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class LinesTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Lines sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Lines();
			sut.setState(state);
		}

		@Test
		void emptyFile() throws IOException {
			Path newFile = temporaryFolder.newFile("data.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void nonEmptyFile() throws IOException {
			Path newFile = temporaryFolder.newFile("data.txt");
			Files.writeString(newFile, "a 1\nb 2\n", StandardCharsets.UTF_8);
			ExitStatus exitStatus = sut.run(List.of(newFile.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("a 1")));
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("b 2")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void nonEmptyFileInCwd() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("data.txt");
			Files.writeString(newFile, "a 1\n");
			ExitStatus exitStatus = sut.run(List.of(newFile.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.TEXT, Values.ofText("a 1")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void directory() {
			ExitStatus exitStatus = sut.run(List.of(temporaryFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not readable file")));
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: lines file")));
			then(out).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class CopyTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Copy sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Copy();
			sut.setState(state);
		}

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cp file file")));
		}

		@Test
		void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("source.txt"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: cp file file")));
		}

		@Test
		void copyRelativeToRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path source = temporaryFolder.newFile("source.txt");
			Path target = temporaryFolder.newFile("target.txt");
			Files.delete(target);
			ExitStatus exitStatus = sut.run(List.of(source.getFileName().toString(), target.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(source).exists();
			assertThat(target).exists();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void copyAbsoluteToAbsolute() throws IOException {
			Path source = temporaryFolder.newFile("source.txt").toAbsolutePath();
			Path target = temporaryFolder.newFile("target.txt").toAbsolutePath();
			Files.delete(target);
			ExitStatus exitStatus = sut.run(List.of(source.toString(), target.toString()), in, out, err);
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
	class MoveTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Move sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Move();
			sut.setState(state);
		}

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: mv file file")));
		}

		@Test
		void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("source.txt"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: mv file file")));
		}

		@Test
		void moveRelativeToRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path source = temporaryFolder.newFile("source.txt");
			Path target = temporaryFolder.newFile("target.txt");
			Files.delete(target);
			ExitStatus exitStatus = sut.run(List.of(source.getFileName().toString(), target.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(source).doesNotExist();
			assertThat(target).exists();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void moveAbsoluteToAbsolute() throws IOException {
			Path source = temporaryFolder.newFile("source.txt");
			Path target = temporaryFolder.newFile("target.txt");
			Files.delete(target);
			ExitStatus exitStatus = sut.run(List.of(source.toAbsolutePath().toString(), target.toAbsolutePath().toString()), in, out, err);
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
	class RemoveTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Remove sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Remove();
			sut.setState(state);
		}

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: rm file")));
		}

		@Test
		void removeRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path target = temporaryFolder.newFile("target.txt");
			ExitStatus exitStatus = sut.run(List.of(target.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(target).doesNotExist();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void removeAbsolute() throws IOException {
			Path target = temporaryFolder.newFile("target.txt");
			ExitStatus exitStatus = sut.run(List.of(target.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			assertThat(target).doesNotExist();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class PartitionsTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Partitions sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Partitions();
		}

		@Test
		void listAllPartitions() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(any()); // not very precise test... to be improved
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void tooManyArgs() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: partitions")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class WalkTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Walk sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Walk();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: walk directory")));
			then(out).shouldHaveNoInteractions();
		}

		@Test
		void emptyRelativeDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void nonEmptyRelativeDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of("."), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(Keys.PATH, Values.ofPath(newFile.toAbsolutePath()), Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void nonExistentRelativeDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("path"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not found")));
		}

		@Test
		void nonEmptyAbsoluteDirectory() throws IOException {
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(temporaryFolder.toPath().toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(Keys.PATH, Values.ofPath(newFile.toAbsolutePath()), Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void absoluteFile() throws IOException {
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a directory")));
		}

		@Test
		void resolveSymlinks() throws IOException {
			Path newFolder = temporaryFolder.newFolder("folder");
			Path newFile = temporaryFolder.newFile(newFolder, "file.txt");
			Path symlink = temporaryFolder.toPath().resolve("symlink");
			Files.createSymbolicLink(symlink, newFolder);
			given(state.getCwd()).willReturn(temporaryFolder.toPath()); // previous method could fail by UAC and Mockito will throw UnnecessaryStubbingException
			ExitStatus exitStatus = sut.run(List.of(symlink.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(RecordMatcher.of(Keys.PATH, Values.ofPath(newFile), Keys.SIZE, Values.ofSize(0)));
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ProbeTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Probe sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Probe();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: probe file")));
		}

		@Test
		void probeKnown() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.of("contenttype"), Values.ofText("text/plain")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void probeUnknown() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file.ppp");
			ExitStatus exitStatus = sut.run(List.of(newFile.getFileName().toString()), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("content type cannot be determined")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class GlobTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Glob sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Glob();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: glob pattern")));
		}

		@SuppressWarnings("unchecked")
		@Test
		void matchRelativePath() {
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
		void matchAbsolutePath() {
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
		void noMatch() {
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
	class SymlinkTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Symlink sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Symlink();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: symlink source target")));
		}

		@Test
		void symlinkFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getFileName().toString(), "link"), in, out, err);
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
	class HardlinkTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Hardlink sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Hardlink();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: hardlink source target")));
		}

		@Test
		void symlinkFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.getFileName().toString(), "link"), in, out, err);
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
	class ResolveTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Resolve sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Resolve();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: resolve file")));
		}

		@Test
		void regularRelative() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of("file.txt"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toAbsolutePath().toRealPath())));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void regularAbsolute() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(List.of(newFile.toAbsolutePath().toString()), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toAbsolutePath().toRealPath())));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void symlink() throws IOException {
			Path newFile = temporaryFolder.newFile("file.txt");
			Files.createSymbolicLink(temporaryFolder.toPath().resolve("link"), newFile);
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus exitStatus = sut.run(List.of("link"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.PATH, Values.ofPath(newFile.toAbsolutePath().toRealPath())));
			then(err).shouldHaveNoInteractions();
		}
	}


	@Nested
	@ExtendWith(MockitoExtension.class)
	class WatchTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FileSystemModule.Watch sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.Watch();
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus result = sut.run(List.of(), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: watch directory")));
		}

		@Test
		void twoArgs() {
			ExitStatus result = sut.run(List.of("a", "b"), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: watch directory")));
		}

		@Test
		void oneArg() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			sut.setWatchPredicate(count -> false); // exit immediately
			ExitStatus result = sut.run(List.of(temporaryFolder.toPath().toString()), in, out, err);
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		// this test is quite slow (for the sleep) and race condition prone...
		// perhaps a CountDownLatch could be useful to avoid the race condition?
		@Test
		void create() throws InterruptedException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());

			sut.setWatchPredicate(count -> count < 1);

			Thread modifyFileSystem = new Thread(() -> {
				try {
					TimeUnit.SECONDS.sleep(2);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				try {
					temporaryFolder.newFile("watcher.test");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			modifyFileSystem.start();

			ExitStatus result = sut.run(List.of(temporaryFolder.toPath().toString()), in, out, err);
			modifyFileSystem.join();
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
					Records.builder()
							.entry(Keys.of("type"), Values.ofText("CREATE"))
							.entry(Keys.PATH, Values.ofPath(Paths.get("watcher.test")))
							.build(),
					EnumSet.of(OutputChannel.Option.DIRECT)
			);
			then(err).shouldHaveNoInteractions();

		}


	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class WithLockTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		CommandWrapper.NestedCommand nestedCommand;

		FileSystemModule.WithLock sut;

		@BeforeEach
		void createSut() {
			sut = new FileSystemModule.WithLock();
			sut.setNestedCommand(nestedCommand);
			sut.setState(state);
		}

		@Test
		void noArgs() {
			ExitStatus result = sut.run(List.of(), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: withLock file { ... }")));
		}

		@Test
		void cantCreateFile() {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus result = sut.run(List.of("../missing_directory/file.lock"), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(any(Record.class));
		}

		@Test
		void lock() {
			ExitStatus nestedExitStatus = ExitStatus.of(42);
			given(nestedCommand.run()).willReturn(nestedExitStatus);
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			ExitStatus result = sut.run(List.of("file.lock"), in, out, err);
			assertThat(result).isEqualTo(nestedExitStatus);
			// very weak assertions... please improve them!
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}
}
