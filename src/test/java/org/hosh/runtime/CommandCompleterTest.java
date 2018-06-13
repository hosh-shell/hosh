package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class CommandCompleterTest {

	@Mock
	private LineReader lineReader;

	@Mock
	private ParsedLine parsedLine;

	@Mock
	private State state;

	@Mock
	private Command command;

	@InjectMocks
	private CommandCompleter sut;

	@Test
	public void emptyCommands() {
		List<Candidate> candidates = new ArrayList<>();
		sut.complete(lineReader, parsedLine, candidates);
		then(lineReader).shouldHaveNoMoreInteractions();
		then(parsedLine).shouldHaveNoMoreInteractions();
		assertThat(candidates).isEmpty();
	}

	@Test
	public void oneCommand() {
		List<Candidate> candidates = new ArrayList<>();
		given(state.getCommands()).willReturn(Collections.singletonMap("cmd", command.getClass()));
		sut.complete(lineReader, parsedLine, candidates);
		then(lineReader).shouldHaveNoMoreInteractions();
		then(parsedLine).shouldHaveNoMoreInteractions();
		assertThat(candidates)
				.isNotEmpty()
				.extracting(Candidate::value)
				.allMatch(p -> p.equals("cmd"));
	}

}
