package org.hosh.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.util.List;

import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class FileSystemCompleterTest {
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();
	@Mock
	private State state;
	@Mock(stubOnly = true)
	private LineReader lineReader;
	@Mock
	private ParsedLine line;
	@Mock
	private List<Candidate> candidates;
	@InjectMocks
	private FileSystemCompleter sut;

	@Test
	public void emptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
		given(line.word()).willReturn("");
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void nonEmptyWordInEmptyDir() {
		given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
		given(line.word()).willReturn("aaa");
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void emptyWordInNonEmptyDir() throws IOException {
		temporaryFolder.newFile("a");
		given(state.getCwd()).willReturn(temporaryFolder.getRoot().toPath());
		given(line.word()).willReturn("aaa");
		sut.complete(lineReader, line, candidates);
		then(candidates).should().add(ArgumentMatchers.any());
	}

	@Ignore("fails on windows, / should be C:/")
	@Test
	public void slash() {
		given(line.word()).willReturn("/");
		sut.complete(lineReader, line, candidates);
		then(candidates).should(Mockito.atLeastOnce()).add(ArgumentMatchers.any());
	}

	@Test
	public void absoluteDir() throws IOException {
		String dir = temporaryFolder.getRoot().getAbsolutePath();
		temporaryFolder.newFile();
		given(line.word()).willReturn(dir);
		sut.complete(lineReader, line, candidates);
		then(candidates).should(Mockito.atLeastOnce()).add(ArgumentMatchers.any());
	}
}
