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
import hosh.spi.CommandWrapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

		SystemModule.Path sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Path();
			sut.setState(state);
		}

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

		Exit sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Exit();
			sut.setState(state);
		}

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

		Env sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Env();
			sut.setState(state);
		}

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

		Help sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Help();
			sut.setState(state);
		}

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

		Echo sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Echo();
		}

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

		Sleep sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Sleep();
		}

		@Test
		void interrupts() {
			withThread.interrupt();
			ExitStatus exitStatus = sut.run(List.of("1s"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("interrupted")));
		}

		@Test
		void twoArgs() {
			ExitStatus exitStatus = sut.run(List.of("1", "seconds"), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sleep duration")));
		}

		@Test
		void noArgs() {
			ExitStatus exitStatus = sut.run(List.of(), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: sleep duration")));
		}

		// sleep 100ms
		@ValueSource(strings = {
			"PT0.1S", // ISO 8601
			"PT0.1s", // same but with lowercase suffix
			"0.1S",   // our custom format
			"0.1s",   // our custom format
		})
		@ParameterizedTest
		void validInput(String input) {
			ExitStatus exitStatus = sut.run(List.of(input), in, out, err);
			assertThat(exitStatus).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@EmptySource
		@ValueSource(strings = {
			"PT",   // missing value and unit
			"PT1",  // missing unit
			"1",    // missing unit
			"AAA",  // bogus
			"_",    // bogus
		})
		@ParameterizedTest
		void sleepWithValidIso8601(String input) {
			ExitStatus exitStatus = sut.run(List.of(input), in, out, err);
			assertThat(exitStatus).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("invalid duration: '" + input + "'")));
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

		ProcessList sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.ProcessList();
		}

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

		Err sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Err();
		}

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

		Sink sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Sink();
		}

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
		CommandWrapper.NestedCommand nestedCommand;

		Benchmark sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Benchmark();
			sut.setNestedCommand(nestedCommand);
		}

		@Test
		void onePositiveArg() {
			given(nestedCommand.run()).willReturn(ExitStatus.success());
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
		CommandWrapper.NestedCommand nestedCommand;

		WithTime sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.WithTime();
			sut.setNestedCommand(nestedCommand);
		}

		@Test
		void noArgs() {
			ExitStatus nestedExitStatus = ExitStatus.of(42);
			given(nestedCommand.run()).willReturn(nestedExitStatus);
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
	class WithTimeoutTest {

		@RegisterExtension
		final WithThread withThread = new WithThread();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		CommandWrapper.NestedCommand nestedCommand;

		SystemModule.WithTimeout sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.WithTimeout();
			sut.setNestedCommand(nestedCommand);
		}

		@Test
		void oneArgInterrupted() {
			withThread.interrupt();
			Duration timeout = Duration.ofMillis(200);
			ExitStatus result = sut.run(List.of(timeout.toString()), in, out, err);
			assertThat(withThread.isInterrupted()).isTrue();
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("interrupted")));
		}

		@Test
		void oneArgException() {
			Duration timeout = Duration.ofMillis(200);
			given(nestedCommand.run()).willAnswer((Answer<ExitStatus>) invocationOnMock -> {
				throw new NullPointerException("simulated error"); // could happen for a built-in command
			});
			ExitStatus result = sut.run(List.of(timeout.toString()), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("java.lang.NullPointerException: simulated error")));
		}

		@Test
		void oneArgTimeout() {
			Duration timeout = Duration.ofMillis(200);
			ExitStatus nestedExitStatus = ExitStatus.success();
			given(nestedCommand.run()).willAnswer((Answer<ExitStatus>) invocationOnMock -> {
				// simulating a command slower than the timeout
				Thread.sleep(timeout.toMillis() * 2);
				return nestedExitStatus;
			});
			ExitStatus result = sut.run(List.of(timeout.toString()), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("timeout")));
		}

		@Test
		void oneArgNoTimeout() {
			Duration timeout = Duration.ofMillis(200);
			ExitStatus nestedExitStatus = ExitStatus.success();
			given(nestedCommand.run()).willAnswer((Answer<ExitStatus>) invocationOnMock -> {
				// simulating a command faster than the timeout
				Thread.sleep(1);
				return nestedExitStatus;
			});
			ExitStatus result = sut.run(List.of(timeout.toString()), in, out, err);
			assertThat(result).isEqualTo(nestedExitStatus);
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void noArgs() {
			ExitStatus result = sut.run(List.of(), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: withTimeout duration { ... }")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class WaitSuccessTest {

		@RegisterExtension
		final WithThread withThread = new WithThread();

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		@Mock(stubOnly = true)
		CommandWrapper.NestedCommand nestedCommand;

		SystemModule.WaitSuccess sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.WaitSuccess();
			sut.setNestedCommand(nestedCommand);
		}

		@Test
		void oneArgInterrupted() {
			withThread.interrupt();
			Duration sleep = Duration.ofMillis(200);
			ExitStatus nestedExitStatus = ExitStatus.error(); // to trigger at least one sleep()
			given(nestedCommand.run()).willReturn(nestedExitStatus);
			ExitStatus result = sut.run(List.of(sleep.toString()), in, out, err);
			assertThat(result).isError();
			assertThat(withThread.isInterrupted()).isTrue();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(RecordMatcher.of(Keys.ERROR, Values.ofText("interrupted")));
		}

		@Test
		void oneArgSuccessAtFirstAttempt() {
			Duration sleep = Duration.ofMillis(200);
			ExitStatus nestedExitStatus = ExitStatus.success();
			given(nestedCommand.run()).willReturn(nestedExitStatus);
			ExitStatus result = sut.run(List.of(sleep.toString()), in, out, err);
			assertThat(result).isEqualTo(nestedExitStatus);
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void oneArgSuccessAtSecondAttempt() {
			Duration sleep = Duration.ofMillis(200);
			ExitStatus nestedExitStatus = ExitStatus.success();
			given(nestedCommand.run()).willReturn(ExitStatus.error(), nestedExitStatus);
			ExitStatus result = sut.run(List.of(sleep.toString()), in, out, err);
			assertThat(result).isEqualTo(nestedExitStatus);
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
			// testing success at first attempt only in order to avoid 1s wait (default) in unit tests
		void noArgsSuccessAtFirstAttempt() {
			ExitStatus nestedExitStatus = ExitStatus.success();
			given(nestedCommand.run()).willReturn(nestedExitStatus);
			ExitStatus result = sut.run(List.of(), in, out, err);
			assertThat(result).isEqualTo(nestedExitStatus);
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void twoArgs() {
			ExitStatus result = sut.run(List.of("first", "second"), in, out, err);
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: waitSuccess [duration] { ... }")));
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

		SetVariable sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.SetVariable();
			sut.setState(state);
		}

		@Test
		void noArgs() {
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

		UnsetVariable sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.UnsetVariable();
			sut.setState(state);
		}

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

		KillProcess sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.KillProcess();
			sut.setProcessLookup(processLookup);
		}

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

		Capture sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Capture();
			sut.setState(state);
		}

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

		Open sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Open();
			sut.setState(state);
		}

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

		Input sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Input();
			sut.setLineReader(lineReader);
			sut.setState(state);
		}

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

		Secret sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Secret();
			sut.setLineReader(lineReader);
			sut.setState(state);
		}

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

		SystemModule.Confirm sut;

		@BeforeEach
		void createSut() {
			sut = new SystemModule.Confirm();
			sut.setLineReader(lineReader);
		}

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
