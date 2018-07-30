package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CommandResolversTest {
	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();
	@Mock(stubOnly = true)
	private Command command;
	@Mock
	private State state;
	private CommandResolver sut;

	@Before
	public void setup() {
		sut = CommandResolvers.builtinsThenSystem(state);
	}

	@Test
	public void notFound() {
		given(state.getCommands()).willReturn(Collections.emptyMap());
		given(state.getPath()).willReturn(Collections.emptyList());
		Command result = sut.tryResolve("test");
		assertThat(result).isNull();
	}

	@Test
	public void builtin() {
		given(state.getCommands()).willReturn(Collections.singletonMap("test", command));
		Command result = sut.tryResolve("test");
		assertThat(result).isSameAs(command);
	}

	@Test
	public void validAbsolutePath() throws IOException {
		File file = folder.newFile("test");
		file.setExecutable(true);
		given(state.getCommands()).willReturn(Collections.emptyMap());
		Command result = sut.tryResolve(file.getAbsolutePath());
		assertThat(result).isNotNull();
	}

	@Test
	public void invalidAbsolutePath() throws IOException {
		File file = folder.newFile("test");
		given(state.getCommands()).willReturn(Collections.emptyMap());
		Command result = sut.tryResolve(file.getAbsolutePath() + "_invalid");
		assertThat(result).isNull();
	}

	@Test
	public void foundInPath() throws IOException {
		folder.newFile("test").setExecutable(true);
		given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
		given(state.getCommands()).willReturn(Collections.emptyMap());
		Command result = sut.tryResolve("test");
		assertThat(result).isNotNull();
	}

	@Test
	public void notFoundInPath() {
		given(state.getPath()).willReturn(Arrays.asList(folder.getRoot().toPath().toAbsolutePath()));
		given(state.getCommands()).willReturn(Collections.emptyMap());
		Command result = sut.tryResolve("test");
		assertThat(result).isNull();
	}
}
