package org.hosh.modules;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

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
@SuiteClasses({ FileSystemModuleTest.ListTest.class })
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
		public void emptyDirectory() {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());

			sut.run(Arrays.asList(), out, err);

			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void error() {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());

			sut.run(Arrays.asList("A123"), out, err);

			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			temporaryFolder.newFolder("dir").mkdirs();

			sut.run(Arrays.asList(), out, err);

			then(out).should().send(Record.of("name", Values.ofPath(Paths.get("dir"))));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			temporaryFolder.newFile("file").createNewFile();

			sut.run(Arrays.asList(), out, err);

			then(out).should().send(Record.of("name", Values.ofPath(Paths.get("file"))).add("size", Values.ofSize(0, Unit.B)));
			then(err).shouldHaveZeroInteractions();
		}
		
		@Test
		public void wrongCurrentDirectory() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.newFile().toPath());

			sut.run(Arrays.asList(), out, err);

			then(err).should().send(ArgumentMatchers.any());
			then(out).shouldHaveZeroInteractions();
		}

	}

}
