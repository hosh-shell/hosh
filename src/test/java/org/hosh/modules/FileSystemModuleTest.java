package org.hosh.modules;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.hosh.modules.FileSystemModule.Cat;
import org.hosh.modules.FileSystemModule.ChangeDirectory;
import org.hosh.modules.FileSystemModule.CurrentWorkingDirectory;
import org.hosh.modules.FileSystemModule.ListFiles;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.hosh.spi.Values.Unit;
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
		FileSystemModuleTest.CatTest.class
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
			sut.run(Arrays.asList("dir1", "dir2"), out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("message", Values.ofText("expected at most 1 argument")));
		}

		@Test
		public void zeroArgsWithEmptyDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			sut.run(Arrays.asList(), out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgsWithOneDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			temporaryFolder.newFolder("dir").mkdirs();
			sut.run(Arrays.asList(), out, err);
			then(out).should().send(Record.of("name", Values.ofLocalPath(Paths.get("dir"))));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgsWithOneFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			temporaryFolder.newFile("file").createNewFile();
			sut.run(Arrays.asList(), out, err);
			then(out).should()
					.send(Record.of("name", Values.ofLocalPath(Paths.get("file"))).add("size",
							Values.ofSize(0, Unit.B)));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArgWithFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.newFile().toPath());
			sut.run(Arrays.asList(), out, err);
			then(err).should().send(ArgumentMatchers.any());
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArgWithEmptyDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.newFolder().toPath());
			sut.run(Arrays.asList(), out, err);
			then(err).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoMoreInteractions();
		}

		@Test
		public void oneArgWithNonEmptyDirectory() throws IOException {
			File newFolder = temporaryFolder.newFolder();
			Files.createFile(new File(newFolder, "aaa").toPath());
			given(state.getCwd()).willReturn(newFolder.toPath());
			sut.run(Arrays.asList(), out, err);
			then(err).shouldHaveNoMoreInteractions();
			then(out).should().send(ArgumentMatchers.any());
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
			sut.run(Arrays.asList(), out, err);
			then(err).should().send(Record.of("error", Values.ofText("missing path argument")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("asd", "asd"), out, err);
			then(err).should().send(Record.of("error", Values.ofText("expecting one path argument")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void oneDirectoryRelativeArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			File newFolder = temporaryFolder.newFolder("dir");
			newFolder.mkdirs();
			sut.run(Arrays.asList("dir"), out, err);
			then(state).should().setCwd(newFolder.toPath().toAbsolutePath());
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneDirectoryAbsoluteArgument() throws IOException {
			File newFolder = temporaryFolder.newFolder("dir");
			newFolder.mkdirs();
			sut.run(Arrays.asList(newFolder.toPath().toAbsolutePath().toString()), out, err);
			then(state).should().setCwd(newFolder.toPath());
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneFileArgument() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			File newFile = temporaryFolder.newFile("file");
			newFile.createNewFile();
			sut.run(Arrays.asList("file"), out, err);
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
			sut.run(Arrays.asList(), out, err);
			then(out).should().send(Record.of("cwd", Values.ofLocalPath(temporaryFolder.getRoot().toPath())));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("asd"), out, err);
			then(err).should().send(Record.of("error", Values.ofText("expecting no parameters")));
			then(out).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class CatTest {
		@Rule
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();
		@Mock
		private State state;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Cat sut;

		@Test
		public void emptyFile() throws IOException {
			File newFile = temporaryFolder.newFile("data.txt");
			sut.run(Arrays.asList(newFile.getAbsolutePath()), out, err);
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
			sut.run(Arrays.asList(newFile.getAbsolutePath()), out, err);
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
			sut.run(Arrays.asList(newFile.getName()), out, err);
			then(out).should().send(Record.of("line", Values.ofText("a 1")));
			then(err).shouldHaveNoMoreInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void directory() {
			sut.run(Arrays.asList(temporaryFolder.getRoot().getAbsolutePath()), out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("not readable file")));
		}

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), out, err);
			then(err).should().send(Record.of("error", Values.ofText("expecting one path argument")));
			then(out).shouldHaveZeroInteractions();
		}
	}
}
