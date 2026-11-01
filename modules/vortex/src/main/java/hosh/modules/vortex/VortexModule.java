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

import dev.vortex.api.DataSource;
import dev.vortex.api.Partition;
import dev.vortex.api.Scan;
import dev.vortex.api.ScanOptions;
import dev.vortex.api.Session;
import dev.vortex.arrow.ArrowAllocation;
import dev.vortex.jni.NativeLoader;
import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.spi.Command;
import hosh.spi.CommandArguments;
import hosh.spi.CommandName;
import hosh.spi.CommandRegistry;
import hosh.spi.Errors;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.Module;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Value;
import hosh.spi.Values;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.DateDayVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Field;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class VortexModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand(CommandName.constant("from-vortex"), FromVortex::new);
		registry.registerCommand(CommandName.constant("to-vortex"), ToVortex::new);
	}

	@Description("read a Vortex file into records, one record per row")
	@Examples({
			@Example(description = "read records from a Vortex file", command = "from-vortex data.vortex"),
			@Example(description = "read and count records from a Vortex file", command = "from-vortex data.vortex | count"),
	})
	public static class FromVortex implements Command, StateAware {

		static {
			NativeLoader.loadJni();
		}

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-vortex file"));
				return ExitStatus.error();
			}
			Path source = args.get(0).asPath(state);
			if (!Files.exists(source)) {
				err.send(Errors.message("file not found: %s", source));
				return ExitStatus.error();
			}
			if (!Files.isRegularFile(source)) {
				err.send(Errors.message("not a regular file: %s", source));
				return ExitStatus.error();
			}
			String uri = source.toUri().toString();
			BufferAllocator allocator = ArrowAllocation.rootAllocator();
			try {
				DataSource ds = DataSource.open(Session.create(), uri);
				Scan scan = ds.scan(ScanOptions.of());
				while (scan.hasNext()) {
					Partition partition = scan.next();
					try (ArrowReader reader = partition.scanArrow(allocator)) {
						while (reader.loadNextBatch()) {
							VectorSchemaRoot root = reader.getVectorSchemaRoot();
							List<Field> fields = root.getSchema().getFields();
							int rowCount = root.getRowCount();
							for (int i = 0; i < rowCount; i++) {
								Records.Builder builder = Records.builder();
								for (Field field : fields) {
									FieldVector vector = root.getVector(field.getName());
									Value value = vector.isNull(i) ? Values.none() : toValue(vector, i);
									builder.entry(Keys.of(field.getName()), value);
								}
								out.send(builder.build());
							}
						}
					}
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private Value toValue(FieldVector vector, int index) {
			return switch (vector) {
				case VarCharVector v -> Values.ofText(new String(v.get(index), StandardCharsets.UTF_8));
				case BigIntVector v -> Values.ofNumeric(v.get(index));
				case IntVector v -> Values.ofNumeric(v.get(index));
				case Float8Vector v -> Values.ofText(Double.toString(v.get(index)));
				case Float4Vector v -> Values.ofText(Float.toString(v.get(index)));
				case DateDayVector v -> Values.ofText(LocalDate.ofEpochDay(v.get(index)).toString());
				case BitVector v -> Values.ofText(v.get(index) != 0 ? "true" : "false");
				default -> Values.ofText(vector.getObject(index).toString());
			};
		}
	}

	@Description("write a stream of records to a Vortex file")
	@Examples({
			@Example(description = "save ls output to a Vortex file", command = "ls | to-vortex output.vortex"),
	})
	public static class ToVortex implements Command {

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			throw new UnsupportedOperationException("to-vortex: write support not yet implemented");
		}
	}
}
