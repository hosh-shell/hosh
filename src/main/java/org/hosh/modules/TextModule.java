/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
package org.hosh.modules;

import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hosh.doc.Experimental;
import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Key;
import org.hosh.spi.Keys;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.Value;
import org.hosh.spi.Values;

public class TextModule implements Module {
	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("schema", Schema.class);
		commandRegistry.registerCommand("filter", Filter.class);
		commandRegistry.registerCommand("enumerate", Enumerate.class);
		commandRegistry.registerCommand("sort", Sort.class);
		commandRegistry.registerCommand("distinct", Distinct.class);
		commandRegistry.registerCommand("duplicated", Duplicated.class);
		commandRegistry.registerCommand("take", Take.class);
		commandRegistry.registerCommand("drop", Drop.class);
		commandRegistry.registerCommand("rand", Rand.class);
		commandRegistry.registerCommand("count", Count.class);
		commandRegistry.registerCommand("table", Table.class);
	}

	public static class Schema implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				String schema = record.keys().stream().map(Key::name).collect(Collectors.joining(" "));
				out.send(Record.of(Keys.of("schema"), Values.ofText(schema)));
			}
		}
	}

	public static class Filter implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 2 parameters: key regex")));
				return ExitStatus.error();
			}
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Key key = Keys.of(args.get(0));
				String regex = args.get(1);
				Record record = incoming.get();
				Optional<Value> value = record.value(key);
				if (value.isPresent() && value.get().matches(regex)) {
					out.send(record);
				}
			}
		}
	}

	public static class Enumerate implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			long i = 1;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				out.send(record.prepend(Keys.INDEX, Values.ofNumeric(i++)));
			}
		}
	}

	@Experimental(description = "uniq")
	public static class Distinct implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
				return ExitStatus.error();
			}
			Set<Value> seen = new HashSet<>();
			Key key = Keys.of(args.get(0));
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				record.value(key).ifPresent(v -> {
					boolean neverSeenBefore = seen.add(v);
					if (neverSeenBefore) {
						out.send(record);
					}
				});
			}
			return ExitStatus.success();
		}
	}

	@Experimental(description = "uniq -d")
	public static class Duplicated implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
				return ExitStatus.error();
			}
			Set<Value> seen = new HashSet<>();
			Key key = Keys.of(args.get(0));
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				record.value(key).ifPresent(v -> {
					boolean seenBefore = !seen.add(v);
					if (seenBefore) {
						out.send(record);
					}
				});
			}
			return ExitStatus.success();
		}
	}

	public static class Sort implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			List<Record> records = new ArrayList<>();
			accumulate(in, records);
			sortBy(key, records);
			output(out, records);
			return ExitStatus.success();
		}

		private void accumulate(Channel in, List<Record> records) {
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				records.add(record);
			}
		}

		private void sortBy(Key key, List<Record> records) {
			Comparator<Record> comparator = Comparator.comparing(record -> record.value(key).orElse(null),
					Comparator.nullsFirst(Comparator.naturalOrder()));
			records.sort(comparator);
		}

		private void output(Channel out, List<Record> records) {
			for (Record record : records) {
				out.send(record);
			}
		}
	}

	@Todo(description = "error handling (e.g. non-integer and negative value)")
	public static class Take implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter")));
				return ExitStatus.error();
			}
			int take = Integer.parseInt(args.get(0));
			while (true) {
				if (take == 0) {
					break;
				}
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				out.send(record);
				take--;
			}
			return ExitStatus.success();
		}
	}

	@Todo(description = "error handling (e.g. non-integer and negative value)")
	public static class Drop implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 1 parameter")));
				return ExitStatus.error();
			}
			int drop = Integer.parseInt(args.get(0));
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				if (drop > 0) {
					drop--;
				} else {
					out.send(record);
				}
			}
			return ExitStatus.success();
		}
	}

	@Experimental(description = "extends with seed and bounds parameters, then add also 'doubles', 'booleans', etc")
	public static class Rand implements Command {
		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

		@SuppressWarnings("squid:S2189")
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			SecureRandom secureRandom;
			try {
				secureRandom = SecureRandom.getInstanceStrong();
			} catch (NoSuchAlgorithmException e) {
				LOGGER.log(Level.SEVERE, "failed to get SecureRandom instance", e);
				err.send(Record.of(Keys.ERROR, Values.ofText(e.getMessage())));
				return ExitStatus.error();
			}
			while (true) {
				long next = secureRandom.nextLong();
				Record of = Record.of(Keys.RAND, Values.ofNumeric(next));
				out.send(of);
			}
		}
	}

	public static class Count implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			long count = 0;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					out.send(Record.of(Keys.COUNT, Values.ofNumeric(count)));
					return ExitStatus.success();
				} else {
					count++;
				}
			}
		}
	}

	public static class Table implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			boolean headerSent = false;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				if (!headerSent) {
					sendHeader(record.keys(), out);
					headerSent = true;
				}
				sendRow(record, out);
			}
			return ExitStatus.success();
		}

		@Todo(description = "try to optimize memory usage here")
		private void sendRow(Record record, Channel out) {
			Locale locale = Locale.getDefault();
			StringBuilder formatter = new StringBuilder();
			Collection<Record.Entry> entries = record.entries();
			List<String> formattedValues = new ArrayList<>(entries.size());
			for (Record.Entry entry : entries) {
				StringWriter writer = new StringWriter();
				formatter.append(formatterFor(entry.getKey()));
				entry.getValue().append(writer, locale);
				formattedValues.add(writer.toString());
			}
			String row = String.format(locale, formatter.toString(), formattedValues.toArray());
			out.send(Record.of(Keys.of("row"), Values.ofText(row)));
		}

		private String formatterFor(Key key) {
			return String.format("%%-%ds", paddings.getOrDefault(key, 10));
		}

		private void sendHeader(Collection<Key> keys, Channel out) {
			Locale locale = Locale.getDefault();
			String format = keys.stream()
					.map(this::formatterFor)
					.collect(Collectors.joining(" "));
			String header = String.format(locale, format, keys.stream().map(Key::name).toArray());
			out.send(Record.of(Keys.of("header"), Values.ofText(header)));
		}

		private final Map<Key, Integer> paddings = Map.of(Keys.PATH, 30, Keys.SIZE, 5);
	}
}
