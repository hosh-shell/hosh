package org.hosh.runtime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ExecutableInPathCompleterTest {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Mock(stubOnly = true)
	private State state;

	@Mock(stubOnly = true)
	private LineReader lineReader;

	@Mock(stubOnly = true)
	private ParsedLine line;

	@Mock
	private List<Candidate> candidates;

	@InjectMocks
	private ExecutableInPathCompleter sut;

	@Test
	public void emptyPath() {
		given(state.getPath()).willReturn(List.of());
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void pathWithEmptyDir() {
		given(state.getPath()).willReturn(List.of(temporaryFolder.getRoot().toPath()));
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void pathWithExecutable() throws IOException {
		given(state.getPath()).willReturn(List.of(temporaryFolder.getRoot().toPath()));
		File file = temporaryFolder.newFile("executable");
		assert file.setExecutable(true, true);
		sut.complete(lineReader, line, candidates);
		then(candidates).should()
				.add(DebuggableCandidate.completeWithDescription("executable", "external " + temporaryFolder.getRoot().getAbsolutePath()));
	}

	@Test
	public void skipNonInPathDirectory() throws IOException {
		File file = temporaryFolder.newFile();
		given(state.getPath()).willReturn(List.of(file.toPath()));
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}

	@Test
	public void ioErrorsAreIgnored() throws IOException {
		File bin = temporaryFolder.newFolder("bin");
		bin.setExecutable(false);
		bin.setReadable(false);
		given(state.getPath()).willReturn(List.of(bin.toPath().toAbsolutePath()));
		sut.complete(lineReader, line, candidates);
		then(candidates).shouldHaveZeroInteractions();
	}
}
