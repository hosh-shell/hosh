/**
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hosh.testsupport.ExitStatusAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hosh.modules.SystemModule.Benchmark;
import org.hosh.modules.SystemModule.Benchmark.Accumulator;
import org.hosh.modules.SystemModule.Echo;
import org.hosh.modules.SystemModule.Env;
import org.hosh.modules.SystemModule.Err;
import org.hosh.modules.SystemModule.Exit;
import org.hosh.modules.SystemModule.Help;
import org.hosh.modules.SystemModule.KillProcess;
import org.hosh.modules.SystemModule.KillProcess.ProcessLookup;
import org.hosh.modules.SystemModule.ProcessList;
import org.hosh.modules.SystemModule.SetVariable;
import org.hosh.modules.SystemModule.Sink;
import org.hosh.modules.SystemModule.Sleep;
import org.hosh.modules.SystemModule.Source;
import org.hosh.modules.SystemModule.UnsetVariable;
import org.hosh.modules.SystemModule.WithTime;
import org.hosh.spi.Channel;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.Values;
import org.hosh.testsupport.WithThread;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(Suite.class)
@SuiteClasses({
		SystemModuleTest.EchoTest.class,
		SystemModuleTest.EnvTest.class,
		SystemModuleTest.ExitTest.class,
		SystemModuleTest.HelpTest.class,
		SystemModuleTest.SleepTest.class,
		SystemModuleTest.ProcessListTest.class,
		SystemModuleTest.ErrTest.class,
		SystemModuleTest.BenchmarkTest.class,
		SystemModuleTest.WithTimeTest.class,
		SystemModuleTest.SourceTest.class,
		SystemModuleTest.SinkTest.class,
		SystemModuleTest.SetVariableTest.class,
		SystemModuleTest.UnsetVariableTest.class,
		SystemModuleTest.KillProcessTest.class,
})
public class SystemModuleTest {
	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ExitTest {
		@Mock
		private State state;
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Exit sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus.value()).isEqualTo(0);
			then(state).should().setExit(true);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneValidArg() {
			ExitStatus exitStatus = sut.run(Arrays.asList("21"), in, out, err);
			assertThat(exitStatus.value()).isEqualTo(21);
			then(state).should().setExit(true);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneInvalidArg() {
			ExitStatus exitStatus = sut.run(Arrays.asList("asd"), in, out, err);
			assertThat(exitStatus.value()).isEqualTo(1);
			then(state).shouldHaveZeroInteractions();
			then(in).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("not a valid exit status: asd")));
			then(out).shouldHaveZeroInteractions();
		}

		@Test
		public void twoArgs() {
			ExitStatus exitStatus = sut.run(Arrays.asList("1", "2"), in, out, err);
			assertThat(exitStatus.value()).isEqualTo(1);
			then(state).shouldHaveZeroInteractions();
			then(in).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("too many parameters")));
			then(out).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class EnvTest {
		@Mock(stubOnly = true)
		private State state;
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@Captor
		private ArgumentCaptor<Record> records;
		@InjectMocks
		private Env sut;

		@Test
		public void noArgsWithNoEnvVariables() {
			given(state.getVariables()).willReturn(Collections.emptyMap());
			sut.run(Arrays.asList(), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void noArgsWithSomeEnvVariables() {
			given(state.getVariables()).willReturn(Collections.singletonMap("HOSH_VERSION", "1.0"));
			sut.run(Arrays.asList(), in, out, err);
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(in).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
			Record record = Record.builder().entry(Keys.NAME, Values.ofText("HOSH_VERSION")).entry(Keys.VALUE, Values.ofText("1.0")).build();
			assertThat(records.getAllValues()).contains(record);
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("1"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expecting no parameters")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class HelpTest {
		@Mock
		private Channel in;
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
			sut.run(Arrays.asList(), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveZeroInteractions();
			assertThat(records.getAllValues()).contains(Record.of(Keys.of("command"), Values.ofText("name")));
		}

		@Test
		public void noCommands() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			sut.run(Arrays.asList(), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("1"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expecting no parameters")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class EchoTest {
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Echo sut;

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Record.of(Keys.VALUE, Values.ofText("")));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			sut.run(Arrays.asList("a"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Record.of(Keys.VALUE, Values.ofText("a")));
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("a", "b"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Record.of(Keys.VALUE, Values.ofText("a b")));
			then(err).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SleepTest {
		@Rule
		public final WithThread withThread = new WithThread();
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Sleep sut;

		@Test
		public void interrupts() {
			Thread.currentThread().interrupt();
			ExitStatus exitStatus = sut.run(Arrays.asList("1000"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("interrupted")));
		}

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expecting just one argument millis")));
		}

		@Test
		public void oneArgNumber() {
			sut.run(Arrays.asList("1"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArgNotNumber() {
			sut.run(Arrays.asList("a"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("not millis: a")));
		}

		@Test
		public void twoArgs() {
			sut.run(Arrays.asList("a", "b"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expecting just one argument millis")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ProcessListTest {
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private ProcessList sut;

		@Test
		public void noArgs() {
			sut.run(Arrays.asList(), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should(Mockito.atLeastOnce()).send(Mockito.any());
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArgNumber() {
			sut.run(Arrays.asList("1"), in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expecting zero arguments")));
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class ErrTest {
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Err sut;

		@Test
		public void noArgs() {
			assertThatThrownBy(() -> sut.run(Arrays.asList(), in, out, err))
					.hasMessage("injected error: please do not report")
					.isInstanceOf(NullPointerException.class);
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SourceTest {
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Source sut;

		@Ignore("WIP")
		@Test
		public void canBeInterrupted() {
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SinkTest {
		@Mock(stubOnly = true)
		private Record record;
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Sink sut;

		@SuppressWarnings("unchecked")
		@Test
		public void canBeInterrupted() {
			given(in.recv()).willReturn(Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(Arrays.asList(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).should(Mockito.times(2)).recv();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class BenchmarkTest {
		@Mock
		private State state;
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private Benchmark sut;

		@Test
		public void calculations() {
			List<String> args = Arrays.asList("1");
			Accumulator accumulator = sut.before(args, in, out, err);
			accumulator.getResults().add(Duration.ofMillis(20));
			accumulator.getResults().add(Duration.ofMillis(10));
			sut.after(accumulator, in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(
					Record.builder()
							.entry(Keys.COUNT, Values.ofNumeric(2))
							.entry(Benchmark.BEST, Values.ofDuration(Duration.ofMillis(10)))
							.entry(Benchmark.WORST, Values.ofDuration(Duration.ofMillis(20)))
							.entry(Benchmark.AVERAGE, Values.ofDuration(Duration.ofMillis(15)))
							.build());
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void avoidDivideByZero() {
			List<String> args = Arrays.asList("1");
			Accumulator accumulator = sut.before(args, in, out, err);
			sut.after(accumulator, in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(
					Record.builder()
							.entry(Keys.COUNT, Values.ofNumeric(0))
							.entry(Benchmark.BEST, Values.ofDuration(Duration.ZERO))
							.entry(Benchmark.WORST, Values.ofDuration(Duration.ZERO))
							.entry(Benchmark.AVERAGE, Values.ofDuration(Duration.ZERO))
							.build());
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void oneArg() {
			List<String> args = Arrays.asList("1");
			Accumulator accumulator = sut.before(args, in, out, err);
			boolean retry = sut.retry(accumulator, in, out, err);
			assertThat(retry).isFalse();
			sut.after(accumulator, in, out, err);
			then(in).shouldHaveZeroInteractions();
			then(out).should().send(Mockito.any());
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void zeroArgs() {
			List<String> args = Arrays.asList();
			assertThatThrownBy(() -> sut.before(args, in, out, err))
					.hasMessage("requires one integer arg")
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		public void zeroArg() {
			List<String> args = Arrays.asList("0");
			assertThatThrownBy(() -> sut.before(args, in, out, err))
					.hasMessage("repeat must be > 0")
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		public void runIsMarkedAsError() {
			assertThatThrownBy(() -> sut.run(Collections.emptyList(), in, out, err))
					.isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class WithTimeTest {
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private WithTime sut;

		@Test
		public void usage() {
			Long resource = sut.before(Collections.emptyList(), in, out, err);
			sut.after(resource, in, out, err);
			then(out).should().send(Mockito.any());
			then(in).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}

		@Test
		public void retryIsNotSupported() {
			Long resource = sut.before(Collections.emptyList(), in, out, err);
			assertThat(sut.retry(resource, in, out, err)).isFalse();
		}

		@Test
		public void runIsMarkedAsError() {
			assertThatThrownBy(() -> sut.run(Collections.emptyList(), in, out, err))
					.isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class SetVariableTest {
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@Spy
		private State state = new State();
		@InjectMocks
		private SetVariable sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("requires 2 arguments: key value")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("requires 2 arguments: key value")));
		}

		@Test
		public void createsNewBinding() {
			ExitStatus exitStatus = sut.run(List.of("FOO", "BAR"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "BAR");
		}

		@Test
		public void updatesExistingBinding() {
			state.getVariables().put("FOO", "BAR");
			ExitStatus exitStatus = sut.run(List.of("FOO", "BAZ"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "BAZ");
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class UnsetVariableTest {
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@Spy
		private State state = new State();
		@InjectMocks
		private UnsetVariable sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("requires 1 argument: key")));
		}

		@Test
		public void removesExistingBinding() {
			state.getVariables().put("FOO", "BAR");
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
			assertThat(state.getVariables()).doesNotContainKey("FOO");
		}

		@Test
		public void doesNothingWhenUnexistingBinding() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
			assertThat(state.getVariables()).doesNotContainKey("FOO");
		}
	}

	@RunWith(MockitoJUnitRunner.StrictStubs.class)
	public static class KillProcessTest {
		@Mock(stubOnly = true)
		private ProcessHandle processHandle;
		@Mock(stubOnly = true)
		private ProcessLookup processLookup;
		@Mock
		private Channel in;
		@Mock
		private Channel out;
		@Mock
		private Channel err;
		@InjectMocks
		private KillProcess sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("expecting one argument")));
		}

		@Test
		public void nonNumericPid() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("not a valid pid: FOO")));
		}

		@Test
		public void numericPidOfNotExistingProcess() {
			given(processLookup.of(42L)).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("cannot find pid: 42")));
		}

		@Test
		public void numericPidOfExistingProcessWithDestroyFailure() {
			given(processLookup.of(42L)).willReturn(Optional.of(processHandle));
			given(processHandle.destroy()).willReturn(false);
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).should().send(Record.of(Keys.ERROR, Values.ofText("cannot destroy pid: 42")));
		}

		@Test
		public void numericPidOfExistingProcessWithDestroySuccess() {
			given(processLookup.of(42L)).willReturn(Optional.of(processHandle));
			given(processHandle.destroy()).willReturn(true);
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveZeroInteractions();
			then(out).shouldHaveZeroInteractions();
			then(err).shouldHaveZeroInteractions();
		}
	}
}
