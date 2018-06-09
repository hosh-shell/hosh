package org.hosh.modules;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.util.Arrays;

import org.hosh.modules.FileSystemModule.ListFiles;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
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

			then(out).should().send(Record.of("name", "dir"));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneFile() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
			temporaryFolder.newFile("file").createNewFile();

			sut.run(Arrays.asList(), out, err);

			then(out).should().send(Record.of("name", "file").add("size", "0"));
			then(err).shouldHaveZeroInteractions();
		}

	}

}
