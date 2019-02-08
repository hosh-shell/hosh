/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
package org.hosh.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.hosh.modules.FileSystemModule.ChangeDirectory;
import org.hosh.modules.FileSystemModule.CurrentWorkingDirectory;
import org.hosh.modules.FileSystemModule.Find;
import org.hosh.modules.FileSystemModule.Lines;
import org.hosh.modules.FileSystemModule.ListFiles;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({
		FileSystemModuleTest.ListTest.class,
		FileSystemModuleTest.ChangeDirectoryTest.class,
		FileSystemModuleTest.CurrentWorkingDirectoryTest.class,
		FileSystemModuleTest.LinesTest.class,
		FileSystemModuleTest.FindTest.class
})
public class FileSystemModuleTest {
	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ListTest {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();
		@Mock
		private State state;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private ListFiles sut;

		@Test
		public void errorTwoOrMoreArgs() {
			sut.run(Arrays.asList("dir1", "dir2"), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("message", Values.ofText("expected at most 1 argument")));
		}

		@Test
		public void zeroArgsWithEmptyDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			sut.run(Arrays.asList(), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgsWithOneDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			temporaryFolder.newFolder("dir").mkdirs();
			sut.run(Arrays.asList(), null, out, err);
			then(out).should().send(Record.of("path", Values.ofLocalPath(Paths.get("dir")), "size", Values.none()));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgsWithOneFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			temporaryFolder.newFile("file").createNewFile();
			sut.run(Arrays.asList(), null, out, err);
			then(out).should().send(Record.of("path", Values.ofLocalPath(Paths.get("file"))).append("size", Values.ofHumanizedSize(0)));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArgWithRelativeFile() throws IOException {
			Path file = temporaryFolder.newFile().toPath();
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			sut.run(Arrays.asList(file.getFileName().toString()), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("not a directory: " + file.toAbsolutePath().toString())));
		}

		@Test
		public void oneArgWithEmptyDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.newFolder().toPath());
			sut.run(Arrays.asList("."), null, out, err);
			then(err).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgWithNonEmptyDirectory() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			sut.run(Arrays.asList(newFolder.getName()), null, out, err);
			then(out).should().send(ArgumentMatchers.any());
			then(err).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgWithIsRelativizedToCwd() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(newFolder.toPath());
			sut.run(Arrays.asList("."), null, out, err);
			then(err).shouldHaveNoMoreInteractions();
			then(out).should().send(Record.of("path", Values.ofLocalPath(Paths.get("aaa")), "size", Values.ofHumanizedSize(0)));
		}

		@Test
		public void oneArgAbsoluteFile() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Path path = Files.createFile(new File(newFolder, "aaa").toPath()).toAbsolutePath();
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath().toAbsolutePath());
			sut.run(Arrays.asList(path.toString()), null, out, err);
			then(out).shouldHaveNoMoreInteractions();
			then(err).should().send(Record.of("error", Values.ofText("not a directory: " + path)));
		}

		@Test
		public void oneArgAbsoluteDir() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath().toAbsolutePath());
			sut.run(Arrays.asList(newFolder.getAbsolutePath()), null, out, err);
			then(out).should().send(Record.of("path", Values.ofLocalPath(Paths.get("aaa")), "size", Values.ofHumanizedSize(0)));
			then(err).shouldHaveNoMoreInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ChangeDirectoryTest {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();
		@Mock
		private State state;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private ChangeDirectory sut;

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), null, out, err);
			then(err).should().send(Record.of("error", Values.ofText("missing path argument")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("asd", "asd"), null, out, err);
			then(err).should().send(Record.of("error", Values.ofText("expecting one path argument")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void oneDirectoryRelativeArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			File newFolder = temporaryFolder.newFolder("dir");
			newFolder.mkdirs();
			sut.run(Arrays.asList("dir"), null, out, err);
			then(state).should().setCwd(newFolder.toPath().toAbsolutePath());
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneDirectoryAbsoluteArgument() throws IOException {
			File newFolder = temporaryFolder.newFolder("dir");
			newFolder.mkdirs();
			sut.run(Arrays.asList(newFolder.toPath().toAbsolutePath().toString()), null, out, err);
			then(state).should().setCwd(newFolder.toPath());
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneFileArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			File newFile = temporaryFolder.newFile("file");
			newFile.createNewFile();
			sut.run(Arrays.asList("file"), null, out, err);
			then(state).shouldHaveNoMoreInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("not a directory")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class CurrentWorkingDirectoryTest {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();
		@Mock
		private State state;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private CurrentWorkingDirectory sut;

		@Test
		public void noArgs() {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			sut.run(Arrays.asList(), null, out, err);
			then(out).should().send(Record.of("cwd", Values.ofLocalPath(temporaryFolder.getRoot().toPath())));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), null, out, err);
			then(err).should().send(Record.of("error", Values.ofText("expecting no parameters")));
			then(out).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class LinesTest {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();
		@Mock
		private State state;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Lines sut;

		@Test
		public void emptyFile() throws IOException {
			File newFile = temporaryFolder.newFile("data.txt");
			sut.run(Arrays.asList(newFile.getAbsolutePath()), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void nonEmptyFile() throws IOException {
			File newFile = temporaryFolder.newFile("data.txt");
			try (FileWriter writer = new FileWriter(newFile)) {
				writer.write("a 1\n");
				writer.write("b 2\n");
			}
			sut.run(Arrays.asList(newFile.getAbsolutePath()), null, out, err);
			then(out).should().send(Record.of("line", Values.ofText("a 1")));
			then(out).should().send(Record.of("line", Values.ofText("b 2")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void nonEmptyFileInCwd() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			File newFile = temporaryFolder.newFile("data.txt");
			try (FileWriter writer = new FileWriter(newFile)) {
				writer.write("a 1\n");
			}
			sut.run(Arrays.asList(newFile.getName()), null, out, err);
			then(out).should().send(Record.of("line", Values.ofText("a 1")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void directory() {
			sut.run(Arrays.asList(temporaryFolder.getRoot().getAbsolutePath()), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("not readable file")));
		}

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), null, out, err);
			then(err).should().send(Record.of("error", Values.ofText("expecting one path argument")));
			then(out).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class FindTest {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();
		@Mock
		private State state;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Find sut;

		@Test
		public void noArgs() throws Exception {
			ExitStatus exitStatus = sut.run(Arrays.asList(), null, out, err);
			assertThat(exitStatus.value()).isEqualTo(1);
			then(err).should().send(Record.of("error", Values.ofText("expecting one argument")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void relativePath() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			ExitStatus exitStatus = sut.run(Arrays.asList("."), null, out, err);
			assertThat(exitStatus.value()).isEqualTo(0);
			then(out).should().send(Record.of("path", Values.ofLocalPath(newFile.toPath().toAbsolutePath())));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void nonExistentRelativePath() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			File newFile = temporaryFolder.newFile("file.txt");
			newFile.delete();
			ExitStatus exitStatus = sut.run(Arrays.asList(newFile.getName()), null, out, err);
			assertThat(exitStatus.value()).isEqualTo(1);
			then(err).should().send(Record.of("error", Values.ofText("path does not exist: " + newFile)));
			then(out).shouldHaveZeroInteractions();
		}
	}
}
