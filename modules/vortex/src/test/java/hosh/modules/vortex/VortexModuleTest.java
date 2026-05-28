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
package hosh.modules.vortex;

import dev.vortex.api.Session;
import dev.vortex.api.VortexWriter;
import dev.vortex.arrow.ArrowAllocation;
import hosh.modules.vortex.VortexModule.FromVortex;
import hosh.modules.vortex.VortexModule.ToVortex;
import hosh.spi.CommandArguments;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.Values;
import hosh.test.support.TemporaryFolder;
import org.apache.arrow.c.ArrowArray;
import org.apache.arrow.c.ArrowSchema;
import org.apache.arrow.c.Data;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static hosh.spi.test.support.ExitStatusAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;

class VortexModuleTest {

	@Nested
	@ExtendWith(MockitoExtension.class)
	class FromVortexTest {

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

		FromVortex sut;

		@BeforeEach
		void createSut() {
			sut = new FromVortex();
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
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-vortex file")));
		}

		@Test
		void tooManyArgs() {
			// Given
			// (no setup)
			// When
			ExitStatus result = sut.run(CommandArguments.of("a.vortex", "b.vortex"), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("usage: from-vortex file")));
		}

		@Test
		void fileNotFound() {
			// Given
			Path missing = temporaryFolder.toPath().resolve("missing.vortex").toAbsolutePath();
			// When
			ExitStatus result = sut.run(CommandArguments.of(missing.toString()), in, out, err);
			// Then
			assertThat(result).isError();
			then(in).shouldHaveNoInteractions();
			then(out).shouldHaveNoInteractions();
			then(err).should().send(Records.singleton(Keys.ERROR, Values.ofText("file not found: " + missing)));
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
		void readRecords() throws IOException {
			// Given
			Path file = temporaryFolder.toPath().resolve("test.vortex").toAbsolutePath();
			writeVortexFile(file);
			// When
			ExitStatus result = sut.run(CommandArguments.of(file.toString()), in, out, err);
			// Then
			assertThat(result).isSuccess();
			then(in).shouldHaveNoInteractions();
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("alice")).entry(Keys.of("age"), Values.ofNumeric(30)).build());
			then(out).should().send(Records.builder().entry(Keys.of("name"), Values.ofText("bob")).entry(Keys.of("age"), Values.ofNumeric(25)).build());
			then(err).shouldHaveNoInteractions();
		}

		private static void writeVortexFile(Path path) throws IOException {
			Schema schema = new Schema(List.of(
					Field.notNullable("name", ArrowType.Utf8.INSTANCE),
					Field.notNullable("age", new ArrowType.Int(64, true))
			));
			BufferAllocator allocator = ArrowAllocation.rootAllocator();
			String uri = path.toUri().toString();
			try (VortexWriter writer = VortexWriter.create(Session.create(), uri, schema, Map.of(), allocator);
				 VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator)) {
				VarCharVector nameVec = (VarCharVector) root.getVector("name");
				BigIntVector ageVec = (BigIntVector) root.getVector("age");
				nameVec.allocateNew(2);
				ageVec.allocateNew(2);
				nameVec.setSafe(0, "alice".getBytes(StandardCharsets.UTF_8));
				ageVec.setSafe(0, 30);
				nameVec.setSafe(1, "bob".getBytes(StandardCharsets.UTF_8));
				ageVec.setSafe(1, 25);
				root.setRowCount(2);
				try (ArrowArray arr = ArrowArray.allocateNew(allocator);
					 ArrowSchema arrowSchema = ArrowSchema.allocateNew(allocator)) {
					Data.exportVectorSchemaRoot(allocator, root, null, arr, arrowSchema);
					writer.writeBatch(arr.memoryAddress(), arrowSchema.memoryAddress());
				}
			}
		}
	}

	@Nested
	@ExtendWith(MockitoExtension.class)
	class ToVortexTest {

		@Mock
		InputChannel in;

		@Mock
		OutputChannel out;

		@Mock
		OutputChannel err;

		ToVortex sut;

		@BeforeEach
		void createSut() {
			sut = new ToVortex();
		}

		@Test
		void notYetImplemented() {
			assertThatThrownBy(() -> sut.run(CommandArguments.of("output.vortex"), in, out, err))
					.isInstanceOf(UnsupportedOperationException.class)
					.hasMessage("to-vortex: write support not yet implemented");
		}
	}
}
