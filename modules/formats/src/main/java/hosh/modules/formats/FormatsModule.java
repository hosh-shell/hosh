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

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.spi.Command;
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
import hosh.spi.Values;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FormatsModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand(CommandName.constant("from-json"), FromJson::new);
		registry.registerCommand(CommandName.constant("to-json"), ToJson::new);
		registry.registerCommand(CommandName.constant("from-csv"), FromCsv::new);
		registry.registerCommand(CommandName.constant("to-csv"), ToCsv::new);
	}

	@Description("parse a JSON file containing an array of objects into records")
	@Examples({
			@Example(description = "read records from a JSON file", command = "from-json data.json"),
			@Example(description = "read and count records from a JSON file", command = "from-json data.json | count"),
	})
	public static class FromJson implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-json file"));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.getFirst()));
			if (!Files.exists(source)) {
				err.send(Errors.message("file not found: %s", source));
				return ExitStatus.error();
			}
			if (!Files.isRegularFile(source)) {
				err.send(Errors.message("not a regular file: %s", source));
				return ExitStatus.error();
			}
			try (JsonReader reader = jakarta.json.Json.createReader(Files.newBufferedReader(source, StandardCharsets.UTF_8))) {
				JsonValue root = reader.readValue();
				if (!(root instanceof JsonArray array)) {
					err.send(Errors.message("expected a JSON array"));
					return ExitStatus.error();
				}
				for (JsonValue element : array) {
					if (element instanceof JsonObject obj) {
						Records.Builder builder = Records.builder();
						for (var entry : obj.entrySet()) {
							builder.entry(Keys.of(entry.getKey()), toValue(entry.getValue()));
						}
						out.send(builder.build());
					}
				}
				return ExitStatus.success();
			} catch (jakarta.json.JsonException e) {
				err.send(Errors.message(e));
				return ExitStatus.error();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private hosh.spi.Value toValue(JsonValue jsonValue) {
			return switch (jsonValue.getValueType()) {
				case STRING -> Values.ofText(((JsonString) jsonValue).getString());
				case NUMBER -> {
					JsonNumber num = (JsonNumber) jsonValue;
					yield num.isIntegral() ? Values.ofNumeric(num.longValue()) : Values.ofText(num.toString());
				}
				case TRUE -> Values.ofText("true");
				case FALSE -> Values.ofText("false");
				case NULL -> Values.none();
				default -> Values.ofText(jsonValue.toString());
			};
		}
	}

	@Description("parse a RFC 4180 CSV file with a header row into records")
	@Examples({
			@Example(description = "read records from a CSV file", command = "from-csv data.csv"),
			@Example(description = "read and count records from a CSV file", command = "from-csv data.csv | count"),
	})
	public static class FromCsv implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-csv file"));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.getFirst()));
			if (!Files.exists(source)) {
				err.send(Errors.message("file not found: %s", source));
				return ExitStatus.error();
			}
			if (!Files.isRegularFile(source)) {
				err.send(Errors.message("not a regular file: %s", source));
				return ExitStatus.error();
			}
			CSVFormat format = CSVFormat.RFC4180.builder()
					.setHeader()
					.setSkipHeaderRecord(true)
					.build();
			try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8);
				 CSVParser parser = format.parse(reader)) {
				List<String> headers = parser.getHeaderNames();
				for (CSVRecord record : parser) {
					Records.Builder builder = Records.builder();
					for (String header : headers) {
						builder.entry(Keys.of(header), Values.ofText(record.get(header)));
					}
					out.send(builder.build());
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("write a stream of records to a JSON file as an array of objects")
	@Examples({
			@Example(description = "save ls output to a JSON file", command = "ls | to-json output.json"),
			@Example(description = "save process list to a JSON file", command = "ps | to-json procs.json"),
	})
	public static class ToJson implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("to-json file"));
				return ExitStatus.error();
			}
			Path target = resolveAsAbsolutePath(state.getCwd(), Path.of(args.getFirst()));
			Locale locale = Locale.getDefault();
			JsonWriterFactory writerFactory = jakarta.json.Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));
			try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8);
				 JsonWriter jsonWriter = writerFactory.createWriter(writer)) {
				jakarta.json.JsonArrayBuilder arrayBuilder = jakarta.json.Json.createArrayBuilder();
				for (hosh.spi.Record record : InputChannel.iterate(in)) {
					jakarta.json.JsonObjectBuilder objectBuilder = jakarta.json.Json.createObjectBuilder();
					record.keys().forEach(key -> {
						hosh.spi.Value value = record.value(key).orElse(Values.none());
						if (Values.none().equals(value)) {
							objectBuilder.addNull(key.name());
						} else {
							objectBuilder.add(key.name(), value.show(locale));
						}
					});
					arrayBuilder.add(objectBuilder);
				}
				jsonWriter.writeArray(arrayBuilder.build());
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("write a stream of records to a CSV file, using record keys as header")
	@Examples({
			@Example(description = "save ls output to a CSV file", command = "ls | to-csv output.csv"),
			@Example(description = "save process list to a CSV file", command = "ps | to-csv procs.csv"),
	})
	public static class ToCsv implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("to-csv file"));
				return ExitStatus.error();
			}
			Path target = resolveAsAbsolutePath(state.getCwd(), Path.of(args.getFirst()));
			Locale locale = Locale.getDefault();
			boolean headerWritten = false;
			try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
				CSVFormat format = CSVFormat.RFC4180;
				CSVPrinter printer = null;
				for (hosh.spi.Record record : InputChannel.iterate(in)) {
					if (!headerWritten) {
						List<String> headers = record.keys().map(hosh.spi.Key::name).toList();
						printer = format.builder().setHeader(headers.toArray(new String[0])).build().print(writer);
						headerWritten = true;
					}
					printer.printRecord(record.values().map(v -> v.show(locale)).toList());
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static Path resolveAsAbsolutePath(Path cwd, Path file) {
		if (file.isAbsolute()) {
			return normalized(file);
		}
		return normalized(cwd.resolve(file));
	}

	private static Path normalized(Path path) {
		return path.normalize().toAbsolutePath();
	}
}
