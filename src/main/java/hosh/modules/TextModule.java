/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
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
package hosh.modules;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import hosh.doc.BuiltIn;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Todo;
import hosh.spi.Ansi;
import hosh.spi.Ansi.Style;
import hosh.spi.Channel;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.Key;
import hosh.spi.Keys;
import hosh.spi.Module;
import hosh.spi.Record;
import hosh.spi.Record.Entry;
import hosh.spi.Records;
import hosh.spi.Value;
import hosh.spi.Values;

public class TextModule implements Module {

	@BuiltIn(name = "select", description = "select a subset of keys from a record")
	@Examples({
			@Example(description = "select some keys from TSV file", command = "lines file.tsv | split text '\\t' | select 1 2 3"),
	})
	public static class Select implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			List<Key> keys = args.stream().map(Keys::of).collect(Collectors.toUnmodifiableList());
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				Records.Builder builder = Records.builder();
				keys.forEach(k -> {
					record.value(k).ifPresent(v -> {
						builder.entry(k, v);
					});
				});
				out.send(builder.build());
			}
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "split", description = "convert a line to record with numerical keys by splitting")
	@Examples({
			@Example(description = "tab separated file to records", command = "lines file.tsv | split text '\\t'"),
	})
	public static class Split implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Pattern pattern = Pattern.compile(args.get(1));
			Map<Integer, Key> cachedKeys = new HashMap<>();
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				record.value(key)
						.flatMap(v -> v.unwrap(String.class))
						.ifPresent(str -> {
							out.send(split(pattern, str, cachedKeys));
						});
			}
			return ExitStatus.success();
		}

		private Record split(Pattern pattern, String str, Map<Integer, Key> cachedKeys) {
			int k = 1;
			Records.Builder builder = Records.builder();
			for (String value : pattern.split(str)) {
				Key key = cachedKeys.computeIfAbsent(k, this::makeKey);
				builder.entry(key, Values.ofText(value));
				k++;
			}
			return builder.build();
		}

		private Key makeKey(Integer i) {
			return Keys.of(Integer.toString(i));
		}
	}

	@BuiltIn(name = "join", description = "join record into a single-keyed text record")
	@Examples({
			@Example(description = "record to string", command = "lines file.tsv | split text '\\t' | join ','"),
	})
	public static class Join implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: join separator")));
				return ExitStatus.error();
			}
			String sep = args.get(0);
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				List<Value> values = record.values();
				Locale locale = Locale.getDefault();
				boolean skipSep = true;
				for (Value value : values) {
					if (skipSep) {
						skipSep = false;
					} else {
						sw.append(sep);
					}
					value.print(pw, locale);
				}
				out.send(Records.singleton(Keys.TEXT, Values.ofText(sw.toString())));
			}
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "trim", description = "trim strings")
	@Examples({
			@Example(command = "lines pom.xml | trim text", description = "trim")
	})
	public static class Trim implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 argument")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				out.send(trimByKey(record, key));
			}
			return ExitStatus.success();
		}

		@Todo(description = "this method is recreating entries wastefully, " +
				"perhaps would be possible to introduce some form of structural sharing in Builder")
		private Record trimByKey(Record record, Key key) {
			Records.Builder builder = Records.builder();
			List<Entry> entries = record.entries();
			for (Entry entry : entries) {
				if (entry.getKey().equals(key)) {
					Value value = entry.getValue();
					Value trimmed = value.unwrap(String.class).map(s -> s.trim()).map(s -> Values.ofText(s)).orElse(value);
					builder.entry(key, trimmed);
				} else {
					builder.entry(entry.getKey(), entry.getValue());
				}
			}
			return builder.build();
		}
	}

	@BuiltIn(name = "regex", description = "convert a line to a record using a regex with named groups")
	@Examples({
			@Example(description = "parse k=v format into record", command = "echo \"aaa=bbb\" | regex value '(?<name>.+)=(?<value>.+)' | schema"),
	})
	public static class Regex implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Pattern pattern = Pattern.compile(args.get(1));
			List<String> groupNames = extractNamedGroups(args.get(1));
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				record.value(key).ifPresent(v -> {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					record.print(pw, Locale.getDefault());
					Matcher matcher = pattern.matcher(sw.toString());
					Records.Builder builder = Records.builder();
					if (matcher.find()) {
						for (String groupName : groupNames) {
							builder.entry(Keys.of(groupName), Values.ofText(matcher.group(groupName)));
						}
						out.send(builder.build());
					}
				});
			}
			return ExitStatus.success();
		}

		private List<String> extractNamedGroups(String pattern) {
			Pattern names = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");
			Matcher matcher = names.matcher(pattern);
			List<String> result = new ArrayList<>();
			while (matcher.find()) {
				result.add(matcher.group(1));
			}
			return result;
		}
	}

	@BuiltIn(name = "schema", description = "output keys of incoming records")
	@Examples({
			@Example(command = "ls | schema", description = "output 'path size'"),
			@Example(command = "ls | count | schema", description = "output 'count'"),
	})
	public static class Schema implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				String schema = record.keys().stream().map(Key::name).collect(Collectors.joining(" "));
				out.send(Records.singleton(Keys.of("schema"), Values.ofText(schema)));
			}
		}
	}

	@BuiltIn(name = "filter", description = "copy incoming records to the output only if they match a regex")
	@Examples({
			@Example(command = "lines file.txt | filter text '.*The.*' ", description = "output only lines containing 'The' somewhere"),
	})
	public static class Filter implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments: key regex")));
				return ExitStatus.error();
			}
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Key key = Keys.of(args.get(0));
				Pattern pattern = Pattern.compile(args.get(1));
				Record record = incoming.get();
				// this could be allocation intensive but let's see
				record.value(key)
						.flatMap(v -> v.unwrap(String.class))
						.filter(s -> pattern.matcher(s).matches())
						.ifPresent(v -> {
							out.send(record);
						});
			}
		}
	}

	@BuiltIn(name = "enumerate", description = "prepend 'index' key to all incoming records")
	@Examples({
			@Example(command = "lines file.txt | enumerate", description = "similar to 'cat -n'"),
	})
	public static class Enumerate implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			long i = 1;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				out.send(record.prepend(Keys.INDEX, Values.ofNumeric(i++)));
			}
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "timestamp", description = "prepend 'timestamp' key to all incoming records")
	@Examples({
			@Example(command = "watch | timestamp", description = "tag each event with current timestamp"),
	})
	@Todo(description = "use local time?")
	public static class Timestamp implements Command {

		private Clock clock = Clock.systemUTC();

		public void setClock(Clock clock) {
			this.clock = clock;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				out.send(record.prepend(Keys.TIMESTAMP, Values.ofInstant(clock.instant())));
			}
			return ExitStatus.success();
		}
	}

	@BuiltIn(name = "distinct", description = "only output records that are not repeated in the input according to the specified key")
	@Examples({
			@Example(command = "lines file.txt | distinct text", description = "output all unique lines in 'file.txt'")
	})
	public static class Distinct implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
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

	@BuiltIn(name = "duplicated", description = "only output records that are repeated in the input, according to the specified key")
	@Examples({
			@Example(command = "lines file.txt | duplicated text", description = "output all non-unique lines in 'file.txt'")
	})
	public static class Duplicated implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
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

	@BuiltIn(name = "sort", description = "sort records according to the specified key")
	@Examples({
			@Example(command = "lines file.txt | sort text", description = "sort lines in 'file.txt'")
	})
	public static class Sort implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
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

	@BuiltIn(name = "take", description = "take first n records, discarding everything else")
	@Examples({
			@Example(command = "lines file.txt | take 1", description = "output first line of 'file.txt'")
	})
	public static class Take implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
				return ExitStatus.error();
			}
			int take = Integer.parseInt(args.get(0));
			if (take < 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
				return ExitStatus.error();
			}
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

	@BuiltIn(name = "drop", description = "drop first n records, then keep everything else")
	@Examples({
			@Example(command = "lines file.txt | drop 1", description = "output 'file.txt', except the first line")
	})
	public static class Drop implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
				return ExitStatus.error();
			}
			int drop = Integer.parseInt(args.get(0));
			if (drop < 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
				return ExitStatus.error();
			}
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

	@BuiltIn(name = "rand", description = "create an infinite sequence of random numbers, remember to always limit it with '| take n'")
	@Examples({
			@Example(command = "rand | enumerate | take 3", description = "create 3 random records")
	})
	@Todo(description = "extends with seed, min-max interval; add also 'doubles', 'booleans', etc")
	public static class Rand implements Command {

		@SuppressWarnings("squid:S2189")
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			Random random = ThreadLocalRandom.current();
			while (true) {
				long next = random.nextLong();
				Record of = Records.singleton(Keys.RAND, Values.ofNumeric(next));
				out.send(of);
			}
		}
	}

	@BuiltIn(name = "count", description = "count incoming records")
	@Examples({
			@Example(command = "rand | take 3 | count", description = "output 3")
	})
	public static class Count implements Command {

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			long count = 0;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					out.send(Records.singleton(Keys.COUNT, Values.ofNumeric(count)));
					return ExitStatus.success();
				} else {
					count++;
				}
			}
		}
	}

	@BuiltIn(name = "table", description = "create a nicely formatted table with keys a columns")
	@Examples({
			@Example(command = "rand | enumerate |take 3 | table", description = "output a nicely formatted table")
	})
	@Todo(description = "this is just a proof of concept")
	public static class Table implements Command {

		private int i = 0;

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
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
				PrintWriter printWriter = new PrintWriter(writer);
				formatter.append(formatterFor(entry.getKey()));
				entry.getValue().print(printWriter, locale);
				formattedValues.add(writer.toString());
			}
			String row = String.format(locale, formatter.toString(), formattedValues.toArray());
			out.send(Records.singleton(Keys.of("row"), Values.ofStyledText(row, alternateColor())));
		}

		private Style alternateColor() {
			return ++i % 2 == 0 ? Ansi.Style.BG_WHITE : Ansi.Style.BG_BLUE;
		}

		private String formatterFor(Key key) {
			return String.format("%%-%ds", paddings.getOrDefault(key, 10));
		}

		private void sendHeader(Collection<Key> keys, Channel out) {
			Locale locale = Locale.getDefault();
			String format = keys.stream()
					.map(this::formatterFor)
					.collect(Collectors.joining());
			String header = String.format(locale, format, keys.stream().map(Key::name).toArray());
			out.send(Records.singleton(Keys.of("header"), Values.ofStyledText(header, Ansi.Style.FG_CYAN)));
		}

		@Todo(description = "be aware of width of unicode char, see https://github.com/joshuarubin/wcwidth9")
		private final Map<Key, Integer> paddings = Map.of(
				Keys.NAME, 30,
				Keys.PATH, 30,
				Keys.SIZE, 10,
				Keys.TIMESTAMP, 30,
				Keys.of("hwaddress"), 20);
	}
}
