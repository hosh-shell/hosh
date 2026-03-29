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
package hosh.modules.checksum;

import hosh.modules.checksum.ChecksumModule.FromChecksum;
import hosh.modules.checksum.ChecksumModule.ToChecksum;
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
import hosh.spi.Records;
import hosh.spi.State;
import hosh.test.support.TemporaryFolder;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class ChecksumModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ToChecksumTest {

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

		ToChecksum sut;

		@BeforeEach
		void createSut() {
			sut = new ToChecksum();
			sut.setState(state);
		}

		@Test
		void missingArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of(), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: to-checksum MD5|SHA-1|SHA-256|SHA-512 file")));
		}

		@Test
		void unsupportedAlgorithm() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("CRC32", "file.txt"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
		}

		@Test
		void fileNotFound() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			// When
			ExitStatus result = sut.run(CommandArguments.of("SHA-256", "missing.txt"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
		}

		@Test
		void notARegularFile() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			// When
			ExitStatus result = sut.run(CommandArguments.of("SHA-256", temporaryFolder.toPath().toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
		}

		@Test
		void sha256OfKnownContent() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("data.txt");
			Files.writeString(file, "hello", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of("SHA-256", file.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(file))
					.entry(Keys.of("algorithm"), Values.ofText("SHA-256"))
					.entry(Keys.of("hash"), Values.ofText("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void md5OfKnownContent() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("data.txt");
			Files.writeString(file, "hello", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of("MD5", file.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(file))
					.entry(Keys.of("algorithm"), Values.ofText("MD5"))
					.entry(Keys.of("hash"), Values.ofText("5d41402abc4b2a76b9719d911017c592"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void sha1OfKnownContent() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("data.txt");
			Files.writeString(file, "hello", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of("SHA-1", file.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(file))
					.entry(Keys.of("algorithm"), Values.ofText("SHA-1"))
					.entry(Keys.of("hash"), Values.ofText("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void sha512OfKnownContent() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("data.txt");
			Files.writeString(file, "hello", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of("SHA-512", file.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(file))
					.entry(Keys.of("algorithm"), Values.ofText("SHA-512"))
					.entry(Keys.of("hash"), Values.ofText("9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void pipelineModeHashesPathRecords() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path file = temporaryFolder.newFile("data.txt");
			Files.writeString(file, "hello", StandardCharsets.UTF_8);
			given(in.recv())
					.willReturn(Optional.of(Records.singleton(Keys.PATH, Values.ofPath(file))))
					.willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("SHA-256"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(file))
					.entry(Keys.of("algorithm"), Values.ofText("SHA-256"))
					.entry(Keys.of("hash"), Values.ofText("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void pipelineModeSkipsRecordsWithoutPath() {
			// Given
			given(in.recv())
					.willReturn(Optional.of(Records.singleton(Keys.of("name"), Values.ofText("alice"))))
					.willReturn(Optional.empty());
			// When
			ExitStatus result = sut.run(CommandArguments.of("SHA-256"), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("record has no path key, skipping")));
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class FromChecksumTest {

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

		FromChecksum sut;

		@BeforeEach
		void createSut() {
			sut = new FromChecksum();
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
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-checksum file")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("a.sha256", "b.sha256"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-checksum file")));
		}

		@Test
		void fileNotFound() {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			// When
			ExitStatus result = sut.run(CommandArguments.of("missing.sha256"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
		}

		@Test
		void parseSha256ChecksumFile() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path dataFile = temporaryFolder.newFile("data.txt");
			Path checksumFile = temporaryFolder.newFile("checksums.sha256");
			Files.writeString(checksumFile,
					"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824  data.txt\n",
					StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(checksumFile.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(dataFile))
					.entry(Keys.of("algorithm"), Values.ofText("SHA-256"))
					.entry(Keys.of("hash"), Values.ofText("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void parseMd5ChecksumFile() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path dataFile = temporaryFolder.newFile("data.txt");
			Path checksumFile = temporaryFolder.newFile("checksums.md5");
			Files.writeString(checksumFile,
					"5d41402abc4b2a76b9719d911017c592  data.txt\n",
					StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(checksumFile.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(dataFile))
					.entry(Keys.of("algorithm"), Values.ofText("MD5"))
					.entry(Keys.of("hash"), Values.ofText("5d41402abc4b2a76b9719d911017c592"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void parseBinaryModeChecksumFile() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path dataFile = temporaryFolder.newFile("data.bin");
			Path checksumFile = temporaryFolder.newFile("checksums.sha256");
			Files.writeString(checksumFile,
					"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824 *data.bin\n",
					StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(checksumFile.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(dataFile))
					.entry(Keys.of("algorithm"), Values.ofText("SHA-256"))
					.entry(Keys.of("hash"), Values.ofText("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void skipsBlankLines() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path dataFile = temporaryFolder.newFile("data.txt");
			Path checksumFile = temporaryFolder.newFile("checksums.sha256");
			Files.writeString(checksumFile,
					"\n2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824  data.txt\n\n",
					StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(checksumFile.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder()
					.entry(Keys.PATH, Values.ofPath(dataFile))
					.entry(Keys.of("algorithm"), Values.ofText("SHA-256"))
					.entry(Keys.of("hash"), Values.ofText("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
					.build());
			then(err).shouldHaveNoInteractions();
		}

		@Test
		void unrecognizedLineFormat() throws IOException {
			// Given
			given(state.getCwd()).willReturn(temporaryFolder.toPath());
			Path checksumFile = temporaryFolder.newFile("bad.sha256");
			Files.writeString(checksumFile, "this is not a valid checksum line\n", StandardCharsets.UTF_8);
			// When
			ExitStatus result = sut.run(CommandArguments.of(checksumFile.getFileName().toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
		}
	}
}
