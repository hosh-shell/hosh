/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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
package hosh.modules.system;

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.modules.system.SystemModule.Benchmark;
import hosh.modules.system.SystemModule.Benchmark.Accumulator;
import hosh.modules.system.SystemModule.Capture;
import hosh.modules.system.SystemModule.Echo;
import hosh.modules.system.SystemModule.Env;
import hosh.modules.system.SystemModule.Err;
import hosh.modules.system.SystemModule.Exit;
import hosh.modules.system.SystemModule.Help;
import hosh.modules.system.SystemModule.Input;
import hosh.modules.system.SystemModule.KillProcess;
import hosh.modules.system.SystemModule.KillProcess.ProcessLookup;
import hosh.modules.system.SystemModule.Open;
import hosh.modules.system.SystemModule.ProcessList;
import hosh.modules.system.SystemModule.Secret;
import hosh.modules.system.SystemModule.SetVariable;
import hosh.modules.system.SystemModule.Sink;
import hosh.modules.system.SystemModule.Sleep;
import hosh.modules.system.SystemModule.UnsetVariable;
import hosh.modules.system.SystemModule.WithTime;
import hosh.spi.Ansi;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.test.support.RecordMatcher;
import hosh.test.support.TemporaryFolder;
import hosh.test.support.WithThread;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static hosh.test.support.ExitStatusAssert.assertThat;

public class SystemModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ExitTest {

		@Spy
		private final State state = new State();

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Exit sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).hasExitCode(0);
			assertThat(state.isExit()).isTrue();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneValidArg() {
			ExitStatus exitStatus = sut.run(List.of("21"), in, out, err);
			assertThat(exitStatus).hasExitCode(21);
			assertThat(state.isExit()).isTrue();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneInvalidArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).hasExitCode(1);
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a valid exit status: asd")));
			then(out).shouldHaveNoInteractions();
		}

		@Test
		public void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("1", "2"), in, out, err);
			assertThat(exitStatus).hasExitCode(1);
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("too many arguments")));
			then(out).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class EnvTest {

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Captor
		private ArgumentCaptor<Record> records;

		@InjectMocks
		private Env sut;

		@Test
		public void noArgsWithNoEnvVariables() {
			given(state.getVariables()).willReturn(Collections.emptyMap());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void noArgsWithSomeEnvVariables() {
			given(state.getVariables()).willReturn(Map.of("HOSH_VERSION", "1.0"));
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveNoInteractions();
			Record record = Records.builder().entry(Keys.NAME, Values.ofText("HOSH_VERSION")).entry(Keys.VALUE, Values.ofText("1.0")).build();
			assertThat(records.getAllValues()).contains(record);
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting no arguments")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class HelpTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Mock(stubOnly = true)
		private State state;

		@Captor
		private ArgumentCaptor<Record> records;

		@InjectMocks
		private Help sut;

		@Test
		public void specificCommandWithExamples() {
			given(state.getCommands()).willReturn(Map.of("true", True::new));
			ExitStatus exitStatus = sut.run(List.of("true"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveNoInteractions();
			assertThat(records.getAllValues())
				.containsExactly(
					Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("true - /bin/true replacement"), Ansi.Style.BOLD)),
					Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("Examples"), Ansi.Style.BOLD)),
					Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("true # returns exit success"), Ansi.Style.ITALIC)));
		}

		@Test
		public void specificCommandWithoutExamples() {
			given(state.getCommands()).willReturn(Map.of("false", False::new));
			ExitStatus exitStatus = sut.run(List.of("false"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(records.capture());
			then(err).shouldHaveNoInteractions();
			assertThat(records.getAllValues())
				.containsExactly(
					Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("false - /bin/false replacement"), Ansi.Style.BOLD)),
					Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("Examples"), Ansi.Style.BOLD)),
					Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText("N/A"), Ansi.Style.FG_RED)));
		}

		@Test
		public void commandNotFound() {
			ExitStatus exitStatus = sut.run(List.of("test"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("command not found: test")));
		}

		@Test
		public void commandWithoutHelpAnnotation() {
			given(state.getCommands()).willReturn(Map.of("*", Star::new));
			ExitStatus exitStatus = sut.run(List.of("*"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("no help for command: *")));
		}

		@Test
		public void listAllCommands() {
			given(state.getCommands()).willReturn(Map.of("true", True::new));
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(
				RecordMatcher.of(Keys.NAME, Values.ofText("true"), Keys.DESCRIPTION, Values.ofText("/bin/true replacement")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void listNoCommands() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("test", "aaa"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("too many arguments")));
		}

		@Description("/bin/true replacement")
		@Examples({
			@Example(command = "true", description = "returns exit success")
		})
		private class True implements Command {

			@Override
			public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
				return ExitStatus.success();
			}
		}

		@Description("/bin/false replacement")
		private class False implements Command {

			@Override
			public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
				return ExitStatus.error();
			}
		}

		// no help and no examples
		private class Star implements Command {

			@Override
			public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
				return ExitStatus.of(42);
			}
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class EchoTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Echo sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.VALUE, Values.ofText("")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("a"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.VALUE, Values.ofText("a")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("a", "b"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.VALUE, Values.ofText("a b")));
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SleepTest {

		@RegisterExtension
		public final WithThread withThread = new WithThread();

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Sleep sut;

		@Test
		public void interrupts() {
			withThread.interrupt();
			ExitStatus exitStatus = sut.run(List.of("1000"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("interrupted")));
		}

		@Test
		public void tooManyArgs() {
			ExitStatus exitStatus = sut.run(List.of("1", "seconds", "extra"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("too many arguments")));
		}

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("missing duration")));
		}

		@Test
		public void oneArgNumber() {
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneArgNotNumber() {
			ExitStatus exitStatus = sut.run(List.of("a"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid amount: a")));
		}

		@Test
		public void twoArgsAmountNotValid() {
			ExitStatus exitStatus = sut.run(List.of("a", "b"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid amount: a")));
		}

		@Test
		public void twoArgsUnitNotValid() {
			ExitStatus exitStatus = sut.run(List.of("1", "asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid unit: asd")));
		}

		@ValueSource(strings = {"nanos", "micros", "millis", "seconds", "minutes", "hours"})
		@ParameterizedTest
		public void sleepWithValidUnitDuration(String unit) {
			ExitStatus exitStatus = sut.run(List.of("0", unit), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@ValueSource(strings = {"PT0M", "PT0S"})
		@ParameterizedTest
		public void sleepWithValidIso8601(String iso8601Spec) {
			ExitStatus exitStatus = sut.run(List.of(iso8601Spec), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void sleepWithInvalidIso8601() {
			ExitStatus exitStatus = sut.run(List.of("PTM"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid amount: PTM")));
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ProcessListTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private ProcessList sut;

		@Test
		public void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(Mockito.any());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneArgNumber() {
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting zero arguments")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class ErrTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Err sut;

		@Test
		public void noArgs() {
			assertThatThrownBy(() -> sut.run(List.of(), in, out, err))
				.hasMessage("please do not report: this is a simulated error")
				.isInstanceOf(NullPointerException.class)
				.satisfies(e -> {
					then(in).shouldHaveNoInteractions();
					then(out).shouldHaveNoInteractions();
					then(err).shouldHaveNoInteractions();
				});
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SinkTest {

		@Mock(stubOnly = true)
		private Record record;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Sink sut;

		@Test
		public void consumeEmpty() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void consumeAll() {
			given(in.recv()).willReturn(Optional.of(record), Optional.of(record), Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class BenchmarkTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Benchmark sut;

		@Test
		public void calculations() {
			List<String> args = List.of("1");
			Accumulator accumulator = sut.before(args, in, out, err);
			accumulator.getResults().add(Duration.ofMillis(20));
			accumulator.getResults().add(Duration.ofMillis(10));
			sut.after(accumulator, in, out, err);
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
				Records.builder()
					.entry(Keys.COUNT, Values.ofNumeric(2))
					.entry(Benchmark.BEST, Values.ofDuration(Duration.ofMillis(10)))
					.entry(Benchmark.WORST, Values.ofDuration(Duration.ofMillis(20)))
					.entry(Benchmark.AVERAGE, Values.ofDuration(Duration.ofMillis(15)))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void avoidDivideByZero() {
			List<String> args = List.of("1");
			Accumulator accumulator = sut.before(args, in, out, err);
			sut.after(accumulator, in, out, err);
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
				Records.builder()
					.entry(Keys.COUNT, Values.ofNumeric(0))
					.entry(Benchmark.BEST, Values.ofDuration(Duration.ZERO))
					.entry(Benchmark.WORST, Values.ofDuration(Duration.ZERO))
					.entry(Benchmark.AVERAGE, Values.ofDuration(Duration.ZERO))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void oneArg() {
			List<String> args = List.of("1");
			Accumulator accumulator = sut.before(args, in, out, err);
			boolean retry = sut.retry(accumulator, in, out, err);
			assertThat(retry).isFalse();
			sut.after(accumulator, in, out, err);
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Mockito.any());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		public void zeroArgs() {
			List<String> args = List.of();
			assertThatThrownBy(() -> sut.before(args, in, out, err))
				.hasMessage("requires one integer arg")
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		public void zeroArg() {
			List<String> args = List.of("0");
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

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class WithTimeTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private WithTime sut;

		@Test
		public void usage() {
			Long resource = sut.before(Collections.emptyList(), in, out, err);
			sut.after(resource, in, out, err);
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Mockito.any());
			then(err).shouldHaveNoInteractions();
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

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SetVariableTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Spy
		private final State state = new State();

		@InjectMocks
		private SetVariable sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: set name value")));
		}

		@Test
		public void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: set name value")));
		}

		@Test
		public void createNewVariable() {
			ExitStatus exitStatus = sut.run(List.of("FOO", "bar"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "bar");
		}

		@Test
		public void updatesExistingVariable() {
			state.getVariables().put("FOO", "baz");
			ExitStatus exitStatus = sut.run(List.of("FOO", "baz"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "baz");
		}

		@Test
		public void invalidVariableName() {
			ExitStatus exitStatus = sut.run(List.of("${", "baz"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
			assertThat(state.getVariables()).isEmpty();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class UnsetVariableTest {

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@Spy
		private final State state = new State();

		@InjectMocks
		private UnsetVariable sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("requires 1 argument: key")));
		}

		@Test
		public void removesExistingBinding() {
			state.getVariables().put("FOO", "BAR");
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).doesNotContainKey("FOO");
		}

		@Test
		public void doesNothingWhenNotExistingBinding() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).doesNotContainKey("FOO");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class KillProcessTest {

		@Mock(stubOnly = true)
		private ProcessHandle processHandle;

		@Mock(stubOnly = true)
		private ProcessLookup processLookup;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private KillProcess sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument")));
		}

		@Test
		public void nonNumericPid() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a valid pid: FOO")));
		}

		@Test
		public void numericPidOfNotExistingProcess() {
			given(processLookup.of(42L)).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("cannot find pid: 42")));
		}

		@Test
		public void numericPidOfExistingProcessWithDestroyFailure() {
			given(processLookup.of(42L)).willReturn(Optional.of(processHandle));
			given(processHandle.destroy()).willReturn(false);
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("cannot destroy pid: 42")));
		}

		@Test
		public void numericPidOfExistingProcessWithDestroySuccess() {
			given(processLookup.of(42L)).willReturn(Optional.of(processHandle));
			given(processHandle.destroy()).willReturn(true);
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class CaptureTest {

		@Spy
		private final State state = new State();

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Capture sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: capture VARNAME")));
		}

		@Test
		public void invalidVariableName() {
			ExitStatus exitStatus = sut.run(List.of("$AAA"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
		}

		@Test
		public void emptyCapture() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "");
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneLine() {
			given(in.recv()).willReturn(
				Optional.of(Records.singleton(Keys.TEXT, Values.ofText("1"))),
				Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "1");
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoLines() {
			given(in.recv()).willReturn(
				Optional.of(Records.singleton(Keys.TEXT, Values.ofText("1"))),
				Optional.of(Records.singleton(Keys.TEXT, Values.ofText("2"))),
				Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "12");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class OpenTest {

		@RegisterExtension
		public final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		private State state;

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Open sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: open filename [WRITE|APPEND|...]")));
		}

		@Test
		public void oneArgs() {
			ExitStatus exitStatus = sut.run(List.of("filename"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: open filename [WRITE|APPEND|...]")));
		}

		@Test
		public void emptyCapture() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("filename", "WRITE", "CREATE"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			List<String> result = Files.readAllLines(Paths.get(temporaryFolder.toPath().toString(), "filename"));
			assertThat(result).isEmpty();
		}

		@SuppressWarnings("unchecked")
		@Test
		public void oneLine() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(
				Optional.of(Records.singleton(Keys.TEXT, Values.ofText("1"))),
				Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("filename", "WRITE", "CREATE"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			List<String> result = Files.readAllLines(Paths.get(temporaryFolder.toPath().toString(), "filename"));
			assertThat(result).containsExactly("1");
		}

		@SuppressWarnings("unchecked")
		@Test
		public void twoLines() throws IOException {
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(
				Optional.of(Records.singleton(Keys.TEXT, Values.ofText("1"))),
				Optional.of(Records.singleton(Keys.TEXT, Values.ofText("2"))),
				Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("filename", "WRITE", "CREATE"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			List<String> result = Files.readAllLines(Paths.get(temporaryFolder.toPath().toString(), "filename"));
			assertThat(result).containsExactly("1", "2");
		}

		@SuppressWarnings("unchecked")
		@Test
		public void appendExisting() throws IOException {
			Path file = temporaryFolder.newFile("filename").toPath();
			Files.write(file, List.of("existing line"), StandardCharsets.UTF_8);
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(
				Optional.of(Records.singleton(Keys.TEXT, Values.ofText("1"))),
				Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("filename", "WRITE", "APPEND"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoMoreInteractions();
			List<String> result = Files.readAllLines(Paths.get(temporaryFolder.toPath().toString(), "filename"));
			assertThat(result).containsExactly("existing line", "1");
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class InputTest {

		@Mock(stubOnly = true)
		private LineReader lineReader;

		@Spy
		private final State state = new State();

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Input sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: input VARIABLE")));
		}

		@Test
		public void invalidVariableName() {
			ExitStatus exitStatus = sut.run(List.of("$"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
		}

		@Test
		public void inputNonEmptyString() {
			given(lineReader.readLine(Mockito.eq("input> "))).willReturn("1");
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "1");
		}

		@Test
		public void emptyInput() {
			given(lineReader.readLine(Mockito.eq("input> "))).willThrow(new EndOfFileException("simulated"));
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).isEmpty();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	public class SecretTest {

		@Mock(stubOnly = true)
		private LineReader lineReader;

		@Spy
		private final State state = new State();

		@Mock
		private InputChannel in;

		@Mock
		private OutputChannel out;

		@Mock
		private OutputChannel err;

		@InjectMocks
		private Secret sut;

		@Test
		public void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: secret VARIABLE")));
		}

		@Test
		public void invalidVariableName() {
			ExitStatus exitStatus = sut.run(List.of("$"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
		}

		@Test
		public void inputNonEmptyString() {
			given(lineReader.readLine(Mockito.eq('\0'))).willReturn("1");
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "1");
		}

		@Test
		public void emptyInput() {
			given(lineReader.readLine(Mockito.eq("input> "))).willThrow(new EndOfFileException("simulated"));
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).isEmpty();
		}
	}
}
