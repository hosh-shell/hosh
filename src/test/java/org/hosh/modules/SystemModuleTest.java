package org.hosh.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.hosh.modules.SystemModule.Env;
import org.hosh.modules.SystemModule.Exit;
import org.hosh.modules.SystemModule.Help;
import org.hosh.spi.Channel;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({
		SystemModuleTest.ExitTest.class,
		SystemModuleTest.EnvTest.class,
		SystemModuleTest.HelpTest.class,
})
public class SystemModuleTest {

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ExitTest {

		@Rule
		public final ExpectedSystemExit expectedSystemExit = ExpectedSystemExit.none();

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@InjectMocks
		private Exit sut;

		@Test
		public void noArgs() {
			expectedSystemExit.expectSystemExitWithStatus(0);
			sut.run(Arrays.asList(), out, err);
		}

		@Test
		public void oneValidArg() {
			expectedSystemExit.expectSystemExitWithStatus(21);
			sut.run(Arrays.asList("21"), out, err);
		}

		@Test
		public void oneInvalidArg() {
			sut.run(Arrays.asList("asd"), out, err);
			then(err).should().send(Record.of("error", Values.ofText("arg must be a number (0-999)")));
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("1", "2"), out, err);
			then(err).should().send(Record.of("error", Values.ofText("too many parameters")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class EnvTest {

		@Rule
		public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@Captor
		private ArgumentCaptor<Record> records;

		@InjectMocks
		private Env sut;

		@Test
		public void noArgs() throws IOException {
			environmentVariables.set("HOSH_VERSION", "1.0");
			sut.run(Arrays.asList(), out, err);
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveZeroInteractions();
			Record record = Record.empty().add("key", Values.ofText("HOSH_VERSION")).add("value", Values.ofText("1.0"));
			assertThat(records.getAllValues()).contains(record);
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("1"), out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expecting no parameters")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class HelpTest {

		@Rule
		public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

		@Mock
		private Channel out;

		@Mock
		private Channel err;

		@Mock
		private State state;

		@Captor
		private ArgumentCaptor<Record> records;

		@InjectMocks
		private Help sut;

		@Test
		public void oneCommand() throws IOException {
			given(state.getCommands()).willReturn(Collections.singletonMap("name", null));
			sut.run(Arrays.asList(), out, err);
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveZeroInteractions();
			assertThat(records.getAllValues()).contains(Record.of("command", Values.ofText("name")));
		}

		@Test
		public void noCommands() throws IOException {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			sut.run(Arrays.asList(), out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("1"), out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expecting no parameters")));
		}
	}

}
