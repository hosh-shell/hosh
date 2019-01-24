package org.hosh.modules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Arrays;
import java.util.Collections;

import org.hosh.modules.SystemModule.Echo;
import org.hosh.modules.SystemModule.Env;
import org.hosh.modules.SystemModule.Exit;
import org.hosh.modules.SystemModule.Help;
import org.hosh.modules.SystemModule.Sleep;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
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
		SystemModuleTest.EchoTest.class,
		SystemModuleTest.EnvTest.class,
		SystemModuleTest.ExitTest.class,
		SystemModuleTest.HelpTest.class,
		SystemModuleTest.SleepTest.class,
})
public class SystemModuleTest {
	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ExitTest {
		@Mock
		private State state;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Exit sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList(), null, out, err);
			assertThat(exitStatus.value()).isEqualTo(0);
			then(state).should().setExit(true);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneValidArg() {
			ExitStatus exitStatus = sut.run(Arrays.asList("21"), null, out, err);
			assertThat(exitStatus.value()).isEqualTo(21);
			then(state).should().setExit(true);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneInvalidArg() {
			ExitStatus exitStatus = sut.run(Arrays.asList("asd"), null, out, err);
			assertThat(exitStatus.value()).isEqualTo(1);
			then(state).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("not a valid exit status: asd")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void twoArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList("1", "2"), null, out, err);
			assertThat(exitStatus.value()).isEqualTo(1);
			then(state).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("too many parameters")));
			then(out).shouldHaveZeroInteractions();
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
		public void noArgs() {
			environmentVariables.set("HOSH_VERSION", "1.0");
			sut.run(Arrays.asList(), null, out, err);
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveZeroInteractions();
			Record record = Record.empty().add("key", Values.ofText("HOSH_VERSION")).add("value", Values.ofText("1.0"));
			assertThat(records.getAllValues()).contains(record);
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("1"), null, out, err);
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
		public void oneCommand() {
			given(state.getCommands()).willReturn(Collections.singletonMap("name", null));
			sut.run(Arrays.asList(), null, out, err);
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveZeroInteractions();
			assertThat(records.getAllValues()).contains(Record.of("command", Values.ofText("name")));
		}

		@Test
		public void noCommands() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			sut.run(Arrays.asList(), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("1"), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expecting no parameters")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class EchoTest {
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Echo sut;

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), null, out, err);
			then(out).should().send(Record.of("text", Values.ofText("")));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("a"), null, out, err);
			then(out).should().send(Record.of("text", Values.ofText("a")));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("a", "b"), null, out, err);
			then(out).should().send(Record.of("text", Values.ofText("ab")));
			then(err).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SleepTest {
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Sleep sut;

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expecting just one argument millis")));
		}

		@Test
		public void oneArgNumber() {
			sut.run(Arrays.asList("1"), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArgNotNumber() {
			sut.run(Arrays.asList("a"), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("not millis: a")));
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("a", "b"), null, out, err);
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of("error", Values.ofText("expecting just one argument millis")));
		}
	}
}
