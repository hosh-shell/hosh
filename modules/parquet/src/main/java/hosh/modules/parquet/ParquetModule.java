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
package hosh.modules.parquet;

import dev.hardwood.InputFile;
import dev.hardwood.metadata.LogicalType;
import dev.hardwood.reader.ParquetFileReader;
import dev.hardwood.reader.RowReader;
import dev.hardwood.schema.ColumnSchema;
import dev.hardwood.schema.FileSchema;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class ParquetModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand(CommandName.constant("from-parquet"), FromParquet::new);
		registry.registerCommand(CommandName.constant("to-parquet"), ToParquet::new);
	}

	@Description("read a Parquet file into records, one record per row")
	@Examples({
			@Example(description = "read records from a Parquet file", command = "from-parquet data.parquet"),
			@Example(description = "read and count records from a Parquet file", command = "from-parquet data.parquet | count"),
	})
	public static class FromParquet implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-parquet file"));
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
			try (InputFile inputFile = InputFile.of(source);
				 ParquetFileReader fileReader = ParquetFileReader.open(inputFile)) {
				FileSchema schema = fileReader.getFileSchema();
				List<ColumnSchema> columns = schema.getColumns();
				try (RowReader rowReader = fileReader.rowReader()) {
					while (rowReader.hasNext()) {
						rowReader.next();
						Records.Builder builder = Records.builder();
						for (ColumnSchema col : columns) {
							int idx = col.columnIndex();
							Value value = rowReader.isNull(idx) ? Values.none() : toValue(col, rowReader.getValue(idx));
							builder.entry(Keys.of(col.name()), value);
						}
						out.send(builder.build());
					}
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private Value toValue(ColumnSchema col, Object obj) {
			return switch (obj) {
				case String s -> Values.ofText(s);
				case Long l -> Values.ofNumeric(l);
				case Integer i -> Values.ofNumeric(i);
				case Boolean b -> Values.ofText(b.toString());
				case Float f -> Values.ofText(Float.toString(f));
				case Double d -> Values.ofText(Double.toString(d));
				case LocalDate ld -> Values.ofText(ld.toString());
				case LocalTime lt -> Values.ofText(lt.toString());
				case Instant ts -> Values.ofInstant(ts);
				case BigDecimal bd -> Values.ofText(bd.toPlainString());
				case UUID uuid -> Values.ofText(uuid.toString());
				case byte[] bytes when col.logicalType() instanceof LogicalType.StringType -> Values.ofText(new String(bytes, StandardCharsets.UTF_8));
				case byte[] bytes -> Values.ofBytes(bytes);
				default -> Values.ofText(String.valueOf(obj));
			};
		}
	}

	@Description("write a stream of records to a Parquet file")
	@Examples({
			@Example(description = "save ls output to a Parquet file", command = "ls | to-parquet output.parquet"),
	})
	public static class ToParquet implements Command {

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			throw new UnsupportedOperationException("to-parquet: write support not yet available in hardwood");
		}
	}
}
