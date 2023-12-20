/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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
package hosh.modules.text;

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Todo;
import hosh.spi.Command;
import hosh.spi.CommandRegistry;
import hosh.spi.Errors;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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
		registry.registerCommand("last", Last::new);
		registry.registerCommand("rand", Rand::new);
		registry.registerCommand("count", Count::new);
		registry.registerCommand("sum", Sum::new);
		registry.registerCommand("freq", Freq::new);
		registry.registerCommand("min", Min::new);
		registry.registerCommand("max", Max::new);
	}

	@Description("select a subset of keys from a record")
	@Examples({
		@Example(description = "select some keys from TSV file", command = "lines file.tsv | split text '\\t' | select 1 2 3"),
	})
	public static class Select implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			List<Key> keys = args.stream().map(Keys::of).toList();
			for (Record record : InputChannel.iterate(in)) {
				Records.Builder builder = Records.builder();
				for (Key k : keys) {
					record.value(k).ifPresent(v -> builder.entry(k, v)); // side effect
				}
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
				err.send(Errors.usage("split key regex"));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Pattern pattern = Pattern.compile(args.get(1));
			Map<Integer, Key> cachedKeys = new HashMap<>();
			for (Record record : InputChannel.iterate(in)) {
				record.value(key)
					.flatMap(v -> v.unwrap(String.class))
					.ifPresent(str -> out.send(split(pattern, str, cachedKeys))); // side effect
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
				err.send(Errors.usage("join separator"));
				return ExitStatus.error();
			}
			String sep = args.get(0);
			Locale locale = Locale.getDefault();
			for (Record record : InputChannel.iterate(in)) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				Iterator<Value> values = record.values().iterator();
				boolean skipSep = true;
				while (values.hasNext()) {
					Value value = values.next();
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
				err.send(Errors.usage("trim key"));
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
			Iterator<Entry> entries = record.entries().iterator();
			while (entries.hasNext()) {
				Entry entry = entries.next();
				if (entry.getKey().equals(key)) {
					builder = builder.entry(key, trim(entry.getValue()));
				} else {
					builder = builder.entry(entry);
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
				err.send(Errors.usage("regex key regex"));
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
				err.send(Errors.usage("schema"));
				return ExitStatus.error();
			}
			for (Record record : InputChannel.iterate(in)) {
				String schema = record.keys().map(Key::name).collect(Collectors.joining(" "));
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
				err.send(Errors.usage("filter key regex"));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Pattern pattern = Pattern.compile(args.get(1));
			for (Record record : InputChannel.iterate(in)) {
				// this could be allocation intensive but let's see
				record.value(key)
					.flatMap(v -> v.unwrap(String.class))
					.filter(s -> pattern.matcher(s).matches())
					.ifPresent(v -> out.send(record)); // side effect
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
				err.send(Errors.usage("enumerate"));
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
				err.send(Errors.usage("timestamp"));
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
				err.send(Errors.usage("distinct key"));
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
				err.send(Errors.usage("duplicated key"));
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
		@Example(command = "lines file.txt | sort text desc", description = "sort lines in 'file.txt' in descending order"),
		@Example(command = "lines file.txt | sort text asc", description = "sort lines in 'file.txt' in ascending order")
	})
	public static class Sort implements Command {

		private static final String ASC = "asc";
		private static final String DESC = "desc";

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() == 0 || args.size() > 2) {
				err.send(Errors.usage("sort key [%s|%s]", ASC, DESC));
				return ExitStatus.error();
			}
			String direction;
			Key key = Keys.of(args.get(0));
			if (args.size() == 1) {
				direction = ASC;
			} else { // implies size == 2
				String order = args.get(1);
				Optional<String> validate = validate(order);
				if (validate.isEmpty()) {
					err.send(Errors.message("must be '%s' or '%s'", ASC, DESC));
					return ExitStatus.error();
				}
				direction = validate.get();
			}
			List<Record> records = new ArrayList<>();
			accumulate(in, records);
			sortBy(order(key, direction), records);
			output(out, records);
			return ExitStatus.success();
		}

		private Optional<String> validate(String s) {
			if (ASC.equals(s)) {
				return Optional.of(s);
			} else if (DESC.equals(s)) {
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
			Comparator<Record> comparator = Comparator.comparing(record -> record.value(key).orElse(Values.none()), Values.Comparators.noneLast(Comparator.naturalOrder()));
			if (direction.equals(DESC)) {
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
				err.send(Errors.usage("take number"));
				return ExitStatus.error();
			}
			long take = Long.parseLong(args.get(0));
			if (take < 0) {
				err.send(Errors.message("number must be >= 0"));
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
				err.send(Errors.usage("drop number"));
				return ExitStatus.error();
			}
			long drop = Long.parseLong(args.get(0));
			if (drop < 0) {
				err.send(Errors.message("number must be >= 0"));
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

	@Description("keep last n records")
	@Examples({
		@Example(command = "lines file.txt | last 1", description = "output only last line of 'file.txt'")
	})
	public static class Last implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("last number"));
				return ExitStatus.error();
			}
			long n = Long.parseLong(args.get(0));
			if (n < 1) {
				err.send(Errors.message("number must be >= 1"));
				return ExitStatus.error();
			}
			Queue<Record> queue = keepLastRecords(in, n);
			output(out, queue);
			return ExitStatus.success();
		}

		private void output(OutputChannel out, Queue<Record> queue) {
			for (Record record : queue) {
				out.send(record);
			}
		}

		private Queue<Record> keepLastRecords(InputChannel in, long n) {
			Queue<Record> queue = new LinkedList<>();
			for (Record record : InputChannel.iterate(in)) {
				queue.add(record);
				if (queue.size() > n) {
					queue.remove();
				}
			}
			return queue;
		}
	}

	@Description("create an infinite sequence of random numbers, remember to always limit it with '| take n'")
	@Examples({
		@Example(command = "rand | enumerate | take 3", description = "create 3 random records")
	})
	public static class Rand implements Command {

		@SuppressWarnings("squid:S2245")
		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Errors.usage("rand"));
				return ExitStatus.error();
			}
			Random random = ThreadLocalRandom.current();
			while (!Thread.interrupted()) {
				long next = random.nextLong();
				Record of = Records.singleton(Keys.RAND, Values.ofNumeric(next));
				out.send(of);
			}
			return ExitStatus.success();
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
				err.send(Errors.usage("count"));
				return ExitStatus.error();
			}
			long count = 0;
			for (Record ignored : InputChannel.iterate(in)) {
				count += 1;
			}
			out.send(Records.singleton(Keys.COUNT, Values.ofNumeric(count)));
			return ExitStatus.success();
		}
	}

	@Description("calculate sum")
	@Examples({
		@Example(command = "ls /tmp | sum size", description = "calculate size of /tmp directory (non-recursively)"),
		@Example(command = "walk /tmp | sum size", description = "calculate size of /tmp directory (recursively)")
	})
	public static class Sum implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("sum key"));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Optional<Value> result = Optional.empty();
			for (Record record : InputChannel.iterate(in)) {
				Optional<Value> value = record.value(key);
				if (value.isEmpty()) {
					continue;
				}
				if (result.isEmpty()) {
					result = value;
				} else {
					result = result.flatMap(v -> v.merge(value.get()));
				}
			}
			out.send(Records.singleton(key, result.orElse(Values.none())));
			return ExitStatus.success();
		}
	}

	@Description("calculate frequency of values")
	@Examples({
		@Example(command = "lines files.txt | freq text", description = "replaces 'sort file.txt | uniq -c | sort -rn' in UNIX"),
	})
	public static class Freq implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("freq key"));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Map<Value, Long> countByValue = countByValue(in, key);
			output(out, countByValue);
			return ExitStatus.success();
		}

		private Map<Value, Long> countByValue(InputChannel in, Key key) {
			Map<Value, Long> result = new HashMap<>();
			for (Record record : InputChannel.iterate(in)) {
				record.value(key)
					.ifPresent(value -> result.compute(value, (k, count) -> count == null ? 1 : count + 1));
			}
			return result;
		}

		private void output(OutputChannel out, Map<Value, Long> countByValue) {
			for (var kv : countByValue.entrySet()) {
				Record record = Records.builder()
					.entry(Keys.VALUE, kv.getKey())
					.entry(Keys.COUNT, Values.ofNumeric(kv.getValue()))
					.build();
				out.send(record);
			}
		}

	}

	@Description("calculate min of value")
	@Examples({
		@Example(command = "ps | min timestamp", description = "calculate minimum timestamp"),
	})
	public static class Min implements Command {

		public static final Key MIN = Keys.of("min");

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("min key"));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Comparator<Value> comparator = Values.Comparators.noneLast(Comparator.naturalOrder());
			Value min = Values.none();
			for (Record record : InputChannel.iterate(in)) {
				Optional<Value> optionalValue = record.value(key);
				if (optionalValue.isPresent()) {
					Value current = optionalValue.get();
					if (min == null) {
						min = current;
					} else {
						min = comparator.compare(current, min) < 0 ? current : min;
					}
				}
			}
			out.send(Records.singleton(MIN, min));
			return ExitStatus.success();
		}

	}

	@Todo(description = "share implementation with min")
	@Description("calculate max of value")
	@Examples({
		@Example(command = "ps | max pid", description = "calculate max pid"),
	})
	public static class Max implements Command {

		public static final Key MAX = Keys.of("max");

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("max key"));
				return ExitStatus.error();
			}
			Key key = Keys.of(args.get(0));
			Value max = Values.none();
			Comparator<Value> comparator = Values.Comparators.noneFirst(Comparator.naturalOrder());
			for (Record record : InputChannel.iterate(in)) {
				Optional<Value> optionalValue = record.value(key);
				if (optionalValue.isPresent()) {
					Value current = optionalValue.get();
					if (max == null) {
						max = current;
					} else {
						max = comparator.compare(current, max) > 0 ? current : max;
					}
				}
			}
			out.send(Records.singleton(MAX, max));
			return ExitStatus.success();
		}

	}

}
