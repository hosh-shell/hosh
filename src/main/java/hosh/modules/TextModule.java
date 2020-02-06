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
package hosh.modules;

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Experimental;
import hosh.doc.Todo;
import hosh.spi.Ansi;
import hosh.spi.Ansi.Style;
import hosh.spi.Command;
import hosh.spi.CommandRegistry;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Key;
import hosh.spi.Keys;
import hosh.spi.Module;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Record.Entry;
import hosh.spi.Records;
import hosh.spi.Value;
import hosh.spi.Values;

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

public class TextModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand("select", Select::new);
		registry.registerCommand("split", Split::new);
		registry.registerCommand("join", Join::new);
		registry.registerCommand("trim", Trim::new);
		registry.registerCommand("regex", Regex::new);
		registry.registerCommand("schema", Schema::new);
		registry.registerCommand("filter", Filter::new);
		registry.registerCommand("enumerate", Enumerate::new);
		registry.registerCommand("timestamp", Timestamp::new);
		registry.registerCommand("distinct", Distinct::new);
		registry.registerCommand("duplicated", Duplicated::new);
		registry.registerCommand("sort", Sort::new);
		registry.registerCommand("take", Take::new);
		registry.registerCommand("drop", Drop::new);
		registry.registerCommand("rand", Rand::new);
		registry.registerCommand("count", Count::new);
		registry.registerCommand("sum", Sum::new);
		registry.registerCommand("table", Table::new);
	}

	@Description("select a subset of keys from a record")
	@Examples({
		@Example(description = "select some keys from TSV file", command = "lines file.tsv | split text '\\t' | select 1 2 3"),
	})
	public static class Select implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			List<Key> keys = args.stream().map(Keys::of).collect(Collectors.toUnmodifiableList());
			for (Record record : InputChannel.iterate(in)) {
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

	@Description("convert a line to record with numerical keys by splitting")
	@Examples({
		@Example(description = "tab separated file to records", command = "lines file.tsv | split text '\\t'"),
	})
	public static class Split implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: split key regex")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Pattern pattern = Pattern.compile(args.get(1));
			Map<Integer, Key> cachedKeys = new HashMap<>();
			for (Record record : InputChannel.iterate(in)) {
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

	@Description("join record into a single-keyed text record")
	@Examples({
		@Example(description = "record to string", command = "lines file.tsv | split text '\\t' | join ','"),
	})
	public static class Join implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: join separator")));
				return ExitStatus.error();
			}
			String sep = args.get(0);
			Locale locale = Locale.getDefault();
			for (Record record : InputChannel.iterate(in)) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				List<Value> values = record.values();
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

	@Description("trim strings")
	@Examples({
		@Example(command = "lines pom.xml | trim text", description = "trim")
	})
	public static class Trim implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 argument")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			for (Record record : InputChannel.iterate(in)) {
				out.send(trimByKey(record, key));
			}
			return ExitStatus.success();
		}

		private Record trimByKey(Record record, Key key) {
			Records.Builder builder = Records.builder();
			List<Entry> entries = record.entries();
			for (Entry entry : entries) {
				if (entry.getKey().equals(key)) {
					builder.entry(key, trim(entry.getValue()));
				} else {
					builder.entry(entry);
				}
			}
			return builder.build();
		}

		private Value trim(Value value) {
			return value.unwrap(String.class)
				       .map(String::trim)
				       .map(Values::ofText)
				       .orElse(value);
		}
	}

	@Description("convert a line to a record using a regex with named groups")
	@Examples({
		@Example(description = "parse k=v format into record", command = "echo \"aaa=bbb\" | regex value '(?<name>.+)=(?<value>.+)' | schema"),
	})
	public static class Regex implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Pattern pattern = Pattern.compile(args.get(1));
			List<String> groupNames = extractNamedGroups(args.get(1));
			for (Record record : InputChannel.iterate(in)) {
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

	@Description("output keys of incoming records")
	@Examples({
		@Example(command = "ls | schema", description = "output 'path size'"),
		@Example(command = "ls | count | schema", description = "output 'count'"),
	})
	public static class Schema implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			for (Record record : InputChannel.iterate(in)) {
				String schema = record.keys().stream().map(Key::name).collect(Collectors.joining(" "));
				out.send(Records.singleton(Keys.of("schema"), Values.ofText(schema)));
			}
			return ExitStatus.success();
		}
	}

	@Description("copy incoming records to the output only if they match a regex")
	@Examples({
		@Example(command = "lines file.txt | filter text '.*The.*' ", description = "output only lines containing 'The' somewhere"),
	})
	public static class Filter implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 2 arguments: key regex")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Pattern pattern = Pattern.compile(args.get(1));
			for (Record record : InputChannel.iterate(in)) {
				// this could be allocation intensive but let's see
				record.value(key)
					.flatMap(v -> v.unwrap(String.class))
					.filter(s -> pattern.matcher(s).matches())
					.ifPresent(v -> {
						out.send(record);
					});
			}
			return ExitStatus.success();
		}
	}

	@Description("prepend 'index' key to all incoming records")
	@Examples({
		@Example(command = "lines file.txt | enumerate", description = "similar to 'cat -n'"),
	})
	public static class Enumerate implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			long index = 1;
			for (Record record : InputChannel.iterate(in)) {
				out.send(record.prepend(Keys.INDEX, Values.ofNumeric(index)));
				index += 1;
			}
			return ExitStatus.success();
		}
	}

	@Description("prepend 'timestamp' key to all incoming records")
	@Examples({
		@Example(command = "watch | timestamp", description = "tag each event with current timestamp"),
	})
	public static class Timestamp implements Command {

		private Clock clock = Clock.systemUTC();

		public void setClock(Clock clock) {
			this.clock = clock;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			for (Record record : InputChannel.iterate(in)) {
				out.send(record.prepend(Keys.TIMESTAMP, Values.ofInstant(clock.instant())));
			}
			return ExitStatus.success();
		}
	}

	@Description("only output records that are not repeated in the input according to the specified key")
	@Examples({
		@Example(command = "lines file.txt | distinct text", description = "output all unique lines in 'file.txt'")
	})
	public static class Distinct implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
				return ExitStatus.error();
			}
			Set<Value> seen = new HashSet<>();
			Key key = Keys.of(args.get(0));
			for (Record record : InputChannel.iterate(in)) {
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

	@Description("only output records that are repeated in the input, according to the specified key")
	@Examples({
		@Example(command = "lines file.txt | duplicated text", description = "output all non-unique lines in 'file.txt'")
	})
	public static class Duplicated implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter: key")));
				return ExitStatus.error();
			}
			Set<Value> seen = new HashSet<>();
			Key key = Keys.of(args.get(0));
			for (Record record : InputChannel.iterate(in)) {
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

	@Description("sort records according to the specified key")
	@Examples({
		@Example(command = "lines file.txt | sort text", description = "sort lines in 'file.txt' in ascending order"),
		@Example(command = "lines file.txt | sort desc text", description = "sort lines in 'file.txt' in descending order"),
		@Example(command = "lines file.txt | sort asc text", description = "sort lines in 'file.txt' in ascending order")
	})
	public static class Sort implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() == 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("use 'sort key' or 'sort [asc|desc] key'")));
				return ExitStatus.error();
			}
			if (args.size() > 2) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("too many args")));
				return ExitStatus.error();
			}
			String direction;
			Key key;
			if (args.size() == 1) {
				direction = "asc";
				key = Keys.of(args.get(0));
			} else {
				Optional<String> validate = validate(args.get(0));
				if (validate.isEmpty()) {
					err.send(Records.singleton(Keys.ERROR, Values.ofText("must be asc or desc")));
					return ExitStatus.error();
				}
				direction = validate.get();
				key = Keys.of(args.get(1));
			}
			List<Record> records = new ArrayList<>();
			accumulate(in, records);
			sortBy(order(key, direction), records);
			output(out, records);
			return ExitStatus.success();
		}

		private Optional<String> validate(String s) {
			if ("asc".equals(s)) {
				return Optional.of(s);
			} else if ("desc".equals(s)) {
				return Optional.of(s);
			} else {
				return Optional.empty();
			}
		}

		private void accumulate(InputChannel in, List<Record> records) {
			for (Record record : InputChannel.iterate(in)) {
				records.add(record);
			}
		}

		private Comparator<Record> order(Key key, String direction) {
			Comparator<Record> comparator = Comparator.comparing(record -> record.value(key).orElse(Values.none()), Values.noneLast(Comparator.naturalOrder()));
			if (direction.equals("desc")) {
				return comparator.reversed();
			} else {
				return comparator;
			}
		}

		private void sortBy(Comparator<Record> comparator, List<Record> records) {
			records.sort(comparator);
		}

		private void output(OutputChannel out, List<Record> records) {
			for (Record record : records) {
				out.send(record);
			}
		}
	}

	@Description("take first n records, discarding everything else")
	@Examples({
		@Example(command = "lines file.txt | take 1", description = "output first line of 'file.txt'")
	})
	public static class Take implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
				return ExitStatus.error();
			}
			long take = Long.parseLong(args.get(0));
			if (take < 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
				return ExitStatus.error();
			}
			for (Record record : InputChannel.iterate(in)) {
				if (take == 0) {
					break;
				}
				out.send(record);
				take--;
			}
			return ExitStatus.success();
		}
	}

	@Description("drop first n records, then keep everything else")
	@Examples({
		@Example(command = "lines file.txt | drop 1", description = "output 'file.txt', except the first line")
	})
	public static class Drop implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 1 parameter")));
				return ExitStatus.error();
			}
			long drop = Long.parseLong(args.get(0));
			if (drop < 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("parameter must be >= 0")));
				return ExitStatus.error();
			}
			for (Record record : InputChannel.iterate(in)) {
				if (drop > 0) {
					drop--;
				} else {
					out.send(record);
				}
			}
			return ExitStatus.success();
		}
	}

	@Description("create an infinite sequence of random numbers, remember to always limit it with '| take n'")
	@Examples({
		@Example(command = "rand | enumerate | take 3", description = "create 3 random records")
	})
	@Todo(description = "extends with seed, min-max interval; add also 'doubles', 'booleans', etc")
	public static class Rand implements Command {

		@SuppressWarnings("squid:S2189")
		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
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

	@Description("count incoming records")
	@Examples({
		@Example(command = "rand | take 3 | count", description = "output 3")
	})
	public static class Count implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			long count = 0;
			for (Record record : InputChannel.iterate(in)) {
				count += 1;
			}
			out.send(Records.singleton(Keys.COUNT, Values.ofNumeric(count)));
			return ExitStatus.success();
		}
	}

	@Experimental(description = "implementation works only for 'humanized size' values (i.e. cannot be used for numeric)")
	@Description("calculate sum of size")
	@Examples({
		@Example(command = "ls /tmp | sum size", description = "calculate size of /tmp directory (non-recursively)")
	})
	public static class Sum implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: sum key")));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			long sum = 0;
			for (Record record : InputChannel.iterate(in)) {
				sum += record
					       .value(key)
					       .flatMap(v -> v.unwrap(Long.class))
					       .orElse(0L);
			}
			out.send(Records.singleton(key, Values.ofSize(sum)));
			return ExitStatus.success();
		}
	}

	@Description("create a nicely formatted table with keys a columns")
	@Examples({
		@Example(command = "rand | enumerate | take 3 | table", description = "output a nicely formatted table")
	})
	@Experimental(description = "this is just a proof of concept", issue = "https://github.com/dfa1/hosh/issues/139")
	public static class Table implements Command {

		private int i = 0;

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected 0 arguments")));
				return ExitStatus.error();
			}
			boolean headerSent = false;
			for (Record record : InputChannel.iterate(in)) {
				if (!headerSent) {
					sendHeader(record.keys(), out);
					headerSent = true;
				}
				sendRow(record, out);
			}
			return ExitStatus.success();
		}

		private void sendRow(Record record, OutputChannel out) {
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

		private void sendHeader(Collection<Key> keys, OutputChannel out) {
			Locale locale = Locale.getDefault();
			String format = keys.stream()
				                .map(this::formatterFor)
				                .collect(Collectors.joining());
			String header = String.format(locale, format, keys.stream().map(Key::name).toArray());
			out.send(Records.singleton(Keys.of("header"), Values.ofStyledText(header, Ansi.Style.FG_CYAN)));
		}

		private final Map<Key, Integer> paddings = Map.of(
			Keys.NAME, 30,
			Keys.PATH, 30,
			Keys.SIZE, 10,
			Keys.TIMESTAMP, 30,
			Keys.of("hwaddress"), 20);
	}
}
