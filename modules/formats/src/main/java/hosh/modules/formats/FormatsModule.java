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
import hosh.spi.CommandArguments;
import hosh.spi.CommandName;
import hosh.spi.CommandRegistry;
import hosh.spi.Errors;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.OutputChannel;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Values;
import hosh.spi.Module;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

public class FormatsModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand(CommandName.constant("from-json"), FromJson::new);
		registry.registerCommand(CommandName.constant("to-json"), ToJson::new);
		registry.registerCommand(CommandName.constant("from-csv"), FromCsv::new);
		registry.registerCommand(CommandName.constant("to-csv"), ToCsv::new);
		registry.registerCommand(CommandName.constant("from-base64"), FromBase64::new);
		registry.registerCommand(CommandName.constant("to-base64"), ToBase64::new);
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
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-json file"));
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
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-csv file"));
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
			try (CsvReader<NamedCsvRecord> csvReader = CsvReader.builder().ofNamedCsvRecord(source)) {
				for (NamedCsvRecord record : csvReader) {
					Records.Builder builder = Records.builder();
					for (String header : record.getHeader()) {
						builder.entry(Keys.of(header), Values.ofText(record.getField(header)));
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
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("to-json file"));
				return ExitStatus.error();
			}
			Path target = args.get(0).asPath(state);
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
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("to-csv file"));
				return ExitStatus.error();
			}
			Path target = args.get(0).asPath(state);
			Locale locale = Locale.getDefault();
			boolean headerWritten = false;
			try (CsvWriter csvWriter = CsvWriter.builder().build(target, StandardCharsets.UTF_8)) {
				for (hosh.spi.Record record : InputChannel.iterate(in)) {
					if (!headerWritten) {
						String[] headers = record.keys().map(hosh.spi.Key::name).toArray(String[]::new);
						csvWriter.writeRecord(headers);
						headerWritten = true;
					}
					String[] values = record.values().map(v -> v.show(locale)).toArray(String[]::new);
					csvWriter.writeRecord(values);
				}
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("decode base64-encoded text from stdin")
	@Examples({
			@Example(description = "decode base64 string", command = "echo 'SGVsbG8gV29ybGQ=' | from-base64 text"),
			@Example(description = "decode and show text", command = "echo 'SGVsbG8gV29ybGQ=' | from-base64 text | sort text"),
	})
	public static class FromBase64 implements Command {

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("from-base64 key"));
				return ExitStatus.error();
			}
			hosh.spi.Key key = args.get(0).asKey();
			for (hosh.spi.Record record : InputChannel.iterate(in)) {
				record.value(key)
						.flatMap(v -> v.unwrap(String.class))
						.ifPresent(encoded -> {
							try {
								byte[] decoded = Base64.getDecoder().decode(encoded);
								String decodedText = new String(decoded, StandardCharsets.UTF_8);
								out.send(Records.singleton(key, Values.ofText(decodedText)));
							} catch (IllegalArgumentException e) {
								err.send(Errors.message("invalid base64: %s", e.getMessage()));
							}
						}); // side effect
			}
			return ExitStatus.success();
		}
	}

	@Description("encode text as base64")
	@Examples({
			@Example(description = "encode string to base64", command = "echo 'Hello World' | to-base64 text"),
			@Example(description = "encode and show result", command = "echo 'Hello World' | to-base64 text | sort text"),
	})
	public static class ToBase64 implements Command {

		@Override
		public ExitStatus run(CommandArguments args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("to-base64 key"));
				return ExitStatus.error();
			}
			hosh.spi.Key key = args.get(0).asKey();
			for (hosh.spi.Record record : InputChannel.iterate(in)) {
				record.value(key)
						.flatMap(v -> v.unwrap(String.class))
						.ifPresent(text -> {
							String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
							out.send(Records.singleton(key, Values.ofText(encoded)));
						}); // side effect
			}
			return ExitStatus.success();
		}
	}

}
