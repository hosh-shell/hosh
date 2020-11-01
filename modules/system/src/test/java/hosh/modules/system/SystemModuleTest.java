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
import hosh.spi.CommandDecorator;
import hosh.spi.Errors;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.spi.test.support.RecordMatcher;
import hosh.test.support.TemporaryFolder;
import hosh.test.support.WithThread;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static hosh.spi.test.support.ExitStatusAssert.assertThat;

class SystemModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class PathTest {

		@Spy
		final State state = new State();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		SystemModule.Path sut;

		@Test
		void noSubCommand() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: path [show|clear|append path|prepend path]")));
			then(state).shouldHaveNoInteractions();
		}

		@Test
		void unknownSubCommand() {
			ExitStatus exitStatus = sut.run(List.of("whatever"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: path [show|clear|append path|prepend path]")));
			then(state).shouldHaveNoInteractions();
		}

		@Test
		void showZeroArg() {
			Path sbin = Paths.get("/sbin");
			Path bin = Paths.get("/bin");
			given(state.getPath()).willReturn(List.of(sbin, bin));
			ExitStatus exitStatus = sut.run(List.of("show"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			InOrder inOrder = Mockito.inOrder(out); // order of paths is important!
			then(out).should(inOrder).send(Records.singleton(Keys.PATH, Values.ofPath(sbin)));
			then(out).should(inOrder).send(Records.singleton(Keys.PATH, Values.ofPath(bin)));
			then(out).shouldHaveNoMoreInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void showOneArg() {
			ExitStatus exitStatus = sut.run(List.of("show", "anotherArg"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: path show")));
			then(state).shouldHaveNoInteractions();
		}

		@Test
		void clearZeroArg() {
			Path sbin = Paths.get("/sbin");
			Path bin = Paths.get("/bin");
			state.setPath(new ArrayList<>(List.of(sbin, bin)));
			ExitStatus exitStatus = sut.run(List.of("clear"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getPath()).isEmpty();
		}

		@Test
		void clearOneArg() {
			ExitStatus exitStatus = sut.run(List.of("clear", "anotherArg"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: path clear")));
			then(state).shouldHaveNoInteractions();
		}

		@Test
		void appendOneArg() {
			Path bin = Paths.get("/bin");
			state.setPath(new ArrayList<>(List.of(bin)));
			ExitStatus exitStatus = sut.run(List.of("append", "/usr/local/bin"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getPath()).containsExactly(bin, Path.of("/usr/local/bin"));
		}

		@Test
		void appendZeroArgs() {
			ExitStatus exitStatus = sut.run(List.of("append"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: path append path")));
			then(state).shouldHaveNoInteractions();
		}

		@Test
		void prependOneArg() {
			Path bin = Paths.get("/bin");
			state.setPath(new ArrayList<>(List.of(bin)));
			ExitStatus exitStatus = sut.run(List.of("prepend", "/usr/local/bin"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getPath()).containsExactly(Path.of("/usr/local/bin"), bin);
		}

		@Test
		void prependZeroArgs() {
			ExitStatus exitStatus = sut.run(List.of("prepend"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: path prepend path")));
			then(state).shouldHaveNoInteractions();
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ExitTest {

		@Spy
		final State state = new State();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Exit sut;

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).hasExitCode(0);
			assertThat(state.isExit()).isTrue();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneValidArg() {
			ExitStatus exitStatus = sut.run(List.of("21"), in, out, err);
			assertThat(exitStatus).hasExitCode(21);
			assertThat(state.isExit()).isTrue();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneInvalidArg() {
			ExitStatus exitStatus = sut.run(List.of("asd"), in, out, err);
			assertThat(exitStatus).hasExitCode(1);
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a valid exit status: asd")));
			then(out).shouldHaveNoInteractions();
		}

		@Test
		void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("asd", "fgh"), in, out, err);
			assertThat(exitStatus).hasExitCode(1);
			then(state).shouldHaveNoInteractions();
			then(in).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: exit [value]")));
			then(out).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class EnvTest {

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Captor
		ArgumentCaptor<Record> records;

		@InjectMocks
		Env sut;

		@Test
		void noArgsWithNoEnvVariables() {
			given(state.getVariables()).willReturn(Collections.emptyMap());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void noArgsWithSomeEnvVariables() {
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
		void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: env")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class HelpTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		State state;

		@Captor
		ArgumentCaptor<Record> records;

		@InjectMocks
		Help sut;

		@Test
		void specificCommandWithExamples() {
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
		void specificCommandWithoutExamples() {
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
		void commandNotFound() {
			ExitStatus exitStatus = sut.run(List.of("test"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("command not found: test")));
		}

		@Test
		void commandWithoutHelpAnnotation() {
			given(state.getCommands()).willReturn(Map.of("*", Star::new));
			ExitStatus exitStatus = sut.run(List.of("*"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("no help for command: *")));
		}

		@Test
		void listAllCommands() {
			given(state.getCommands()).willReturn(Map.of("true", True::new));
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(
				RecordMatcher.of(Keys.NAME, Values.ofText("true"), Keys.DESCRIPTION, Values.ofText("/bin/true replacement")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void listNoCommands() {
			given(state.getCommands()).willReturn(Collections.emptyMap());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("test", "aaa"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: help [command]")));
		}

		@Description("/bin/true replacement")
		@Examples({
			@Example(command = "true", description = "returns exit success")
		})
		class True implements Command {

			@Override
			public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
				return ExitStatus.success();
			}
		}

		@Description("/bin/false replacement")
		class False implements Command {

			@Override
			public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
				return ExitStatus.error();
			}
		}

		// no help and no examples
		class Star implements Command {

			@Override
			public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
				return ExitStatus.of(42);
			}
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class EchoTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Echo sut;

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.VALUE, Values.ofText("")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("a"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.VALUE, Values.ofText("a")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("a", "b"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.singleton(Keys.VALUE, Values.ofText("a b")));
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SleepTest {

		@RegisterExtension
		final WithThread withThread = new WithThread();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Sleep sut;

		@Test
		void interrupts() {
			withThread.interrupt();
			ExitStatus exitStatus = sut.run(List.of("1000"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("interrupted")));
		}

		@Test
		void tooManyArgs() {
			ExitStatus exitStatus = sut.run(List.of("1", "seconds", "extra"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sleep [duration|duration unit]")));
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sleep [duration|duration unit]")));
		}

		@Test
		void oneArgNumber() {
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArgNotNumber() {
			ExitStatus exitStatus = sut.run(List.of("a"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid amount: a")));
		}

		@Test
		void twoArgsAmountNotValid() {
			ExitStatus exitStatus = sut.run(List.of("a", "b"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid amount: a")));
		}

		@Test
		void twoArgsUnitNotValid() {
			ExitStatus exitStatus = sut.run(List.of("1", "asd"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid unit: asd")));
		}

		@ValueSource(strings = {"nanos", "micros", "millis", "seconds", "minutes", "hours"})
		@ParameterizedTest
		void sleepWithValidUnitDuration(String unit) {
			ExitStatus exitStatus = sut.run(List.of("0", unit), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@ValueSource(strings = {"PT0M", "PT0S"})
		@ParameterizedTest
		void sleepWithValidIso8601(String iso8601Spec) {
			ExitStatus exitStatus = sut.run(List.of(iso8601Spec), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void sleepWithInvalidIso8601() {
			ExitStatus exitStatus = sut.run(List.of("PTM"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid amount: PTM")));
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ProcessListTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		ProcessList sut;

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should(Mockito.atLeastOnce()).send(any());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArgNumber() {
			ExitStatus exitStatus = sut.run(List.of("1"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: ps")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ErrTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Err sut;

		@Test
		void noArgs() {
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
	class SinkTest {

		@Mock(stubOnly = true)
		Record record;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Sink sut;

		@Test
		void consumeEmpty() {
			given(in.recv()).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoMoreInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@SuppressWarnings("unchecked")
		@Test
		void consumeAll() {
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
	class BenchmarkTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		CommandDecorator.CommandNested commandNested;

		@InjectMocks
		Benchmark sut;

		@Test
		void onePositiveArg() {
			given(commandNested.run()).willReturn(ExitStatus.success());
			ExitStatus result = sut.run(List.of("10"), in, out, err);
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(
				RecordMatcher.of(Keys.COUNT, Values.ofNumeric(10)) // not checking AVERAGE, WORST and BEST
			);
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void noArgs() {
			ExitStatus result = sut.run(List.of(), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: benchmark number { ... }")));
		}

		@Test
		void oneNonPositiveArg() {
			ExitStatus result = sut.run(List.of("0"), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("number must be >= 0")));
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class WithTimeTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		CommandDecorator.CommandNested nested;

		@InjectMocks
		WithTime sut;

		@Test
		void zeroArg() {
			ExitStatus nestedExitStatus = ExitStatus.of(42);
			given(nested.run()).willReturn(nestedExitStatus);
			ExitStatus result = sut.run(List.of(), in, out, err);
			assertThat(result).isEqualTo(nestedExitStatus);
			then(in).shouldHaveNoInteractions();
			then(out).should().send(any(Record.class));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArg() {
			ExitStatus result = sut.run(List.of("arg"), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: withTime { ... }")));
		}

	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class SetVariableTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Spy
		final State state = new State();

		@InjectMocks
		SetVariable sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: set variable value")));
		}

		@Test
		void oneArg() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: set variable value")));
		}

		@Test
		void createNewVariable() {
			ExitStatus exitStatus = sut.run(List.of("FOO", "bar"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "bar");
		}

		@Test
		void updatesExistingVariable() {
			state.getVariables().put("FOO", "baz");
			ExitStatus exitStatus = sut.run(List.of("FOO", "baz"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "baz");
		}

		@Test
		void invalidVariableName() {
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
	class UnsetVariableTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Spy
		final State state = new State();

		@InjectMocks
		UnsetVariable sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: unset variable")));
		}

		@Test
		void removesExistingBinding() {
			state.getVariables().put("FOO", "BAR");
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).doesNotContainKey("FOO");
		}

		@Test
		void doesNothingWhenNotExistingBinding() {
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
	class KillProcessTest {

		@Mock(stubOnly = true)
		ProcessHandle processHandle;

		@Mock(stubOnly = true)
		ProcessLookup processLookup;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		KillProcess sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: kill process")));
		}

		@Test
		void nonNumericPid() {
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a valid pid: FOO")));
		}

		@Test
		void numericPidOfNotExistingProcess() {
			given(processLookup.of(42L)).willReturn(Optional.empty());
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("cannot find pid: 42")));
		}

		@Test
		void numericPidOfExistingProcessWithDestroyFailure() {
			given(processLookup.of(42L)).willReturn(Optional.of(processHandle));
			given(processHandle.destroy()).willReturn(false);
			ExitStatus exitStatus = sut.run(List.of("42"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("cannot destroy pid: 42")));
		}

		@Test
		void numericPidOfExistingProcessWithDestroySuccess() {
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
	class CaptureTest {

		@Spy
		final State state = new State();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Capture sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: capture variable")));
		}

		@Test
		void invalidVariableName() {
			ExitStatus exitStatus = sut.run(List.of("$AAA"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
		}

		@Test
		void emptyCapture() {
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
		void oneLine() {
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
		void twoLines() {
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
	class OpenTest {

		@RegisterExtension
		final TemporaryFolder temporaryFolder = new TemporaryFolder();

		@Mock(stubOnly = true)
		State state;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Open sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: open file [WRITE|APPEND|...]")));
		}

		@Test
		void oneArgs() {
			ExitStatus exitStatus = sut.run(List.of("filename"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: open file [WRITE|APPEND|...]")));
		}

		@Test
		void emptyCapture() throws IOException {
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
		void oneLine() throws IOException {
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
		void twoLines() throws IOException {
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
		void appendExisting() throws IOException {
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
	class InputTest {

		@Mock(stubOnly = true)
		LineReader lineReader;

		@Spy
		final State state = new State();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Input sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: input variable")));
		}

		@Test
		void invalidVariableName() {
			ExitStatus exitStatus = sut.run(List.of("$"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
		}

		@Test
		void inputNonEmptyString() {
			given(lineReader.readLine(Mockito.eq("input> "))).willReturn("1");
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "1");
		}

		@Test
		void emptyInput() {
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
	class SecretTest {

		@Mock(stubOnly = true)
		LineReader lineReader;

		@Spy
		final State state = new State();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		Secret sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: secret variable")));
		}

		@Test
		void invalidVariableName() {
			ExitStatus exitStatus = sut.run(List.of("$"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid variable name")));
		}

		@Test
		void inputNonEmptyString() {
			given(lineReader.readLine(Mockito.eq('\0'))).willReturn("1");
			ExitStatus exitStatus = sut.run(List.of("FOO"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(state.getVariables()).containsEntry("FOO", "1");
		}

		@Test
		void emptyInput() {
			given(lineReader.readLine(Mockito.eq('\0'))).willThrow(new EndOfFileException("simulated"));
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
	class ConfirmTest {

		@Mock(stubOnly = true)
		LineReader lineReader;

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@InjectMocks
		SystemModule.Confirm sut;

		@Test
		void zeroArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: confirm message")));
		}

		@ValueSource(strings = {"yes", "YES", "y", "Y"})
		@ParameterizedTest
		void yes(String answer) {
			given(lineReader.readLine("question (Y/N)? ")).willReturn(answer);
			ExitStatus exitStatus = sut.run(List.of("question"), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@ValueSource(strings = {"no", "NO", "n", "N"})
		@ParameterizedTest
		void no(String answer) {
			given(lineReader.readLine("question (Y/N)? ")).willReturn(answer);
			ExitStatus exitStatus = sut.run(List.of("question"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@ValueSource(strings = {"xx", "aaa", "", "1", "0"})
		@ParameterizedTest
		void invalid(String answer) {
			given(lineReader.readLine("question (Y/N)? ")).willReturn(answer);
			ExitStatus exitStatus = sut.run(List.of("question"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Errors.message("invalid answer"));
		}

		@Test
		void userInterrupt() {
			given(lineReader.readLine("question (Y/N)? ")).willThrow(new UserInterruptException("simulated"));
			ExitStatus exitStatus = sut.run(List.of("question"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void endOfFile() {
			given(lineReader.readLine("question (Y/N)? ")).willThrow(new EndOfFileException("simulated"));
			ExitStatus exitStatus = sut.run(List.of("question"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

	}
}
