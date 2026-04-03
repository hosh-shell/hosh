/*
 * MIT License
 *
 * Copyright (c) 2018-2026 Davide Angelocola
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
package hosh.modules.formats;

import hosh.modules.formats.FormatsModule.FromCsv;
import hosh.modules.formats.FormatsModule.FromJson;
import hosh.modules.formats.FormatsModule.ToCsv;
import hosh.modules.formats.FormatsModule.ToJson;
import hosh.modules.formats.FormatsModule.FromBase64;
import hosh.modules.formats.FormatsModule.ToBase64;
import hosh.spi.Values;
import hosh.spi.CommandArguments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;

import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.test.support.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class FormatsModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ParseJsonTest {

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

		FromJson sut;

		@BeforeEach
		void createSut() {
			sut = new FromJson();
			sut.setState(state);
		}

		@Test
		void missingArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-json file")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("a.json", "b.json"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-json file")));
		}

		@Test
		void notAFile() {
			// Given
			Path dir = temporaryFolder.toPath().toAbsolutePath();
			// When
			ExitStatus result = sut.run(CommandArguments.of(dir.toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a regular file: " + dir)));
		}

		@Test
		void fileNotFound() {
			// Given
			Path missing = temporaryFolder.toPath().resolve("missing.json").toAbsolutePath();
			// When
			ExitStatus result = sut.run(CommandArguments.of(missing.toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("file not found: " + missing)));
		}

		@Test
		void emptyArray() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.json");
			Files.writeString(file, "[]", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void arrayOfObjects() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.json");
			Files.writeString(file, """
					[{"name":"alice","age":30},{"name":"bob","age":25}]
					""", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("alice")).entry(Keys.of("age"), Values.ofNumeric(30)).build());
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("bob")).entry(Keys.of("age"), Values.ofNumeric(25)).build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void nullValue() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.json");
			Files.writeString(file, """
					[{"name":null}]
					""", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.none()).build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void booleanValues() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.json");
			Files.writeString(file, """
					[{"active":true,"deleted":false}]
					""", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder().entry(Keys.of("active"), Values.ofText("true")).entry(Keys.of("deleted"), Values.ofText("false")).build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void notAnArray() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.json");
			Files.writeString(file, """
					{"name":"alice"}
					""", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("expected a JSON array")));
		}

		@Test
		void invalidJson() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.json");
			Files.writeString(file, "not json", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(org.mockito.ArgumentMatchers.argThat(r -> r.value(Keys.ERROR).isPresent()));
		}

		@Test
		void relativePathResolvedAgainstCwd() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("data.json");
			Files.writeString(file, "[]", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ParseCsvTest {

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

		FromCsv sut;

		@BeforeEach
		void createSut() {
			sut = new FromCsv();
			sut.setState(state);
		}

		@Test
		void missingArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-csv file")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("a.csv", "b.csv"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-csv file")));
		}

		@Test
		void notAFile() {
			// Given
			Path dir = temporaryFolder.toPath().toAbsolutePath();
			// When
			ExitStatus result = sut.run(CommandArguments.of(dir.toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("not a regular file: " + dir)));
		}

		@Test
		void fileNotFound() {
			// Given
			Path missing = temporaryFolder.toPath().resolve("missing.csv").toAbsolutePath();
			// When
			ExitStatus result = sut.run(CommandArguments.of(missing.toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("file not found: " + missing)));
		}

		@Test
		void headerOnly() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.csv");
			Files.writeString(file, "name,age\r\n", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void headerAndRows() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.csv");
			Files.writeString(file, "name,age\r\nalice,30\r\nbob,25\r\n", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("alice")).entry(Keys.of("age"), Values.ofText("30")).build());
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("bob")).entry(Keys.of("age"), Values.ofText("25")).build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void quotedFields() throws IOException {
			// Given
			Path file = temporaryFolder.newFile("data.csv");
			Files.writeString(file, "name,address\r\n\"Smith, Jr.\",\"123 Main St\"\r\n", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("Smith, Jr.")).entry(Keys.of("address"), Values.ofText("123 Main St")).build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void relativePathResolvedAgainstCwd() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("data.csv");
			Files.writeString(file, "name\r\n", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ToJsonTest {

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

		ToJson sut;

		@BeforeEach
		void createSut() {
			sut = new ToJson();
			sut.setState(state);
		}

		@Test
		void missingArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: to-json file")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("a.json", "b.json"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: to-json file")));
		}

		@Test
		void emptyStream() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.toPath().resolve("output.json");
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("[");
		}

		@Test
		void singleRecord() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.toPath().resolve("output.json");
			Record record = Records.builder().entry(Keys.of("name"), Values.ofText("alice")).entry(Keys.of("age"), Values.ofNumeric(30)).build();
			given(in.recv())
					.willReturn(Optional.of(record))
					.willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			String content = Files.readString(file, StandardCharsets.UTF_8);
			assertThat(content).contains("\"name\"").contains("alice").contains("\"age\"").contains("30");
		}

		@Test
		void nullValueWrittenAsJsonNull() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.toPath().resolve("output.json");
			Record record = Records.builder().entry(Keys.of("name"), Values.none()).build();
			given(in.recv())
					.willReturn(Optional.of(record))
					.willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(Files.readString(file, StandardCharsets.UTF_8)).contains("\"name\"").contains("null");
		}

		@Test
		void relativePathResolvedAgainstCwd() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("output.json"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(temporaryFolder.toPath().resolve("output.json")).exists();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ToCsvTest {

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

		ToCsv sut;

		@BeforeEach
		void createSut() {
			sut = new ToCsv();
			sut.setState(state);
		}

		@Test
		void missingArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: to-csv file")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("a.csv", "b.csv"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: to-csv file")));
		}

		@Test
		void emptyStream() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.toPath().resolve("output.csv");
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEmpty();
		}

		@Test
		void singleRecord() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.toPath().resolve("output.csv");
			Record record = Records.builder().entry(Keys.of("name"), Values.ofText("alice")).entry(Keys.of("age"), Values.ofNumeric(30)).build();
			given(in.recv())
					.willReturn(Optional.of(record))
					.willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("name,age\r\nalice,30\r\n");
		}

		@Test
		void multipleRecords() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.toPath().resolve("output.csv");
			Record alice = Records.builder().entry(Keys.of("name"), Values.ofText("alice")).entry(Keys.of("age"), Values.ofNumeric(30)).build();
			Record bob = Records.builder().entry(Keys.of("name"), Values.ofText("bob")).entry(Keys.of("age"), Values.ofNumeric(25)).build();
			given(in.recv())
					.willReturn(Optional.of(alice))
					.willReturn(Optional.of(bob))
					.willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("name,age\r\nalice,30\r\nbob,25\r\n");
		}

		@Test
		void valuesWithCommasAreQuoted() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.toPath().resolve("output.csv");
			Record record = Records.builder().entry(Keys.of("name"), Values.ofText("Smith, Jr.")).build();
			given(in.recv())
					.willReturn(Optional.of(record))
					.willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toAbsolutePath().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("name\r\n\"Smith, Jr.\"\r\n");
		}

		@Test
		void relativePathResolvedAgainstCwd() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			given(in.recv()).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("output.csv"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).shouldHaveNoInteractions();
			assertThat(temporaryFolder.toPath().resolve("output.csv")).exists();
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class FromBase64Test {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		FromBase64 sut;

		@BeforeEach
		void createSut() {
			sut = new FromBase64();
		}

		@Test
		void missingArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-base64 key")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("key1", "key2"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-base64 key")));
		}

		@Test
		void decodeBase64() {
			// Given
			hosh.spi.Key key = Keys.of("text");
			hosh.spi.Record input = Records.singleton(key, Values.ofText("SGVsbG8gV29ybGQ="));
			given(in.recv()).willReturn(Optional.of(input)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).should().send(Records.singleton(key, Values.ofText("Hello World")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void decodeEmptyString() {
			// Given
			hosh.spi.Key key = Keys.of("text");
			hosh.spi.Record input = Records.singleton(key, Values.ofText(""));
			given(in.recv()).willReturn(Optional.of(input)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).should().send(Records.singleton(key, Values.ofText("")));
		}

		@Test
		void invalidBase64() {
			// Given
			hosh.spi.Key key = Keys.of("text");
			hosh.spi.Record input = Records.singleton(key, Values.ofText("!!!invalid!!!"));
			given(in.recv()).willReturn(Optional.of(input)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(err).should().send(org.mockito.ArgumentMatchers.argThat(record ->
					record.value(Keys.ERROR).isPresent() &&
					record.value(Keys.ERROR).get().unwrap(String.class).orElse("").contains("invalid base64")
			));
		}

		@Test
		void multipleRecords() {
			// Given
			hosh.spi.Key key = Keys.of("data");
			hosh.spi.Record input1 = Records.singleton(key, Values.ofText("SGVsbG8="));
			hosh.spi.Record input2 = Records.singleton(key, Values.ofText("V29ybGQ="));
			given(in.recv()).willReturn(Optional.of(input1)).willReturn(Optional.of(input2)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("data"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).should().send(Records.singleton(key, Values.ofText("Hello")));
			then(out).should().send(Records.singleton(key, Values.ofText("World")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ToBase64Test {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		ToBase64 sut;

		@BeforeEach
		void createSut() {
			sut = new ToBase64();
		}

		@Test
		void missingArg() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: to-base64 key")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("key1", "key2"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: to-base64 key")));
		}

		@Test
		void encodeBase64() {
			// Given
			hosh.spi.Key key = Keys.of("text");
			hosh.spi.Record input = Records.singleton(key, Values.ofText("Hello World"));
			given(in.recv()).willReturn(Optional.of(input)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			then(out).should().send(Records.singleton(key, Values.ofText("SGVsbG8gV29ybGQ=")));
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void encodeEmptyString() {
			// Given
			hosh.spi.Key key = Keys.of("text");
			hosh.spi.Record input = Records.singleton(key, Values.ofText(""));
			given(in.recv()).willReturn(Optional.of(input)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).should().send(Records.singleton(key, Values.ofText("")));
		}

		@Test
		void multipleRecords() {
			// Given
			hosh.spi.Key key = Keys.of("data");
			hosh.spi.Record input1 = Records.singleton(key, Values.ofText("Hello"));
			hosh.spi.Record input2 = Records.singleton(key, Values.ofText("World"));
			given(in.recv()).willReturn(Optional.of(input1)).willReturn(Optional.of(input2)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("data"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).should().send(Records.singleton(key, Values.ofText("SGVsbG8=")));
			then(out).should().send(Records.singleton(key, Values.ofText("V29ybGQ=")));
		}

		@Test
		void encodeSpecialCharacters() {
			// Given
			hosh.spi.Key key = Keys.of("text");
			hosh.spi.Record input = Records.singleton(key, Values.ofText("Hello\nWorld\t!@#$%"));
			given(in.recv()).willReturn(Optional.of(input)).willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("text"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).should().send(org.mockito.ArgumentMatchers.argThat(record ->
					record.value(key).isPresent() &&
					record.value(key).get().unwrap(String.class).isPresent()
			));
		}
	}
}
