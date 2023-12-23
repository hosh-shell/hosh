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
package hosh.runtime;

import hosh.spi.Ansi;
import hosh.spi.Key;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Value;
import hosh.spi.Values;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Display records in a table automatically (for interactive shell only).
 * <p>
 * All records are buffered (within a predefined size) to be able to determine the longest value for each key.
 * On buffer overflow, all records are sent directly to the next channel.
 * <p>
 * NB: implementation must be thread-safe since while end() is running, send() could be called.
 */
public class AutoTableChannel implements OutputChannel {

	public static final int OVERFLOW = 2_000;
	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
	private static final EnumSet<Option> NONE = EnumSet.noneOf(Option.class);
	private static final int COLUMN_PADDING = 2;
	private final OutputChannel outputChannel;
	private final Queue<Record> records;
	private volatile boolean overflow;

	public AutoTableChannel(OutputChannel outputChannel) {
		this.outputChannel = outputChannel;
		this.records = new ConcurrentLinkedDeque<>();
		this.overflow = false;
	}

	@Override
	public void send(Record record) {
		send(record, NONE);
	}

	@Override
	public void send(Record record, EnumSet<Option> options) {
		if (overflow || options.contains(Option.DIRECT)) {
			// if overflow already happened or the record is "direct"; then let's deliver it immediately
			outputChannel.send(record);
			return;
		}
		records.add(record);
		if (records.size() >= OVERFLOW) {
			LOGGER.fine(() -> "autotable: overflow after " + records.size());
			overflow = true;
			// flush and clear our buffer
			records.forEach(outputChannel::send);
		}
	}

	public void flush() {
		LOGGER.fine(() -> "autotable: end with overflow=" + overflow);
		if (!overflow) {
			Map<Key, Integer> paddings = calculatePaddings(records);
			outputTable(outputChannel, records, paddings);
		}
		records.clear();
		overflow = false;
	}

	private void outputTable(OutputChannel out, Collection<Record> records, Map<Key, Integer> paddings) {
		boolean headerSent = false;
		for (Record record : records) {
			if (!headerSent) {
				sendHeader(paddings, record, out);
				headerSent = true;
			}
			sendRow(paddings, record, out);
		}
	}

	private Map<Key, Integer> calculatePaddings(Collection<Record> records) {
		Map<Key, Integer> maxLengthPerColumn = new HashMap<>();
		for (Record record : records) {
			Iterator<Record.Entry> entries = record.entries().iterator();
			while (entries.hasNext()) {
				Record.Entry entry = entries.next();
				String formattedValue = valueAsString(entry.getValue());
				int valueLength = lengthFor(formattedValue);
				maxLengthPerColumn.compute(entry.getKey(), (k, v) -> v == null ? Math.max(k.name().length(), valueLength) : Math.max(v, valueLength));
			}
		}
		Map<Key, Integer> result = new HashMap<>();
		for (var kv : maxLengthPerColumn.entrySet()) {
			result.put(kv.getKey(), kv.getValue() + COLUMN_PADDING);
		}
		LOGGER.fine(() -> "paddings = " + maxLengthPerColumn);
		return result;
	}

	// there are at least 3 or 4 variants of this method
	private String valueAsString(Value value) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		value.print(printWriter, Locale.getDefault());
		return writer.toString();
	}

	private int lengthFor(String value) {
		return dropAnsi(value).length();
	}

	// dropping ansi escape codes, otherwise length is overestimated
	private String dropAnsi(String maybeAnsi) {
		return maybeAnsi.replaceAll("\u001b\\[[0-9]+m", "");
	}

	private void sendRow(Map<Key, Integer> paddings, Record record, OutputChannel out) {
		Locale locale = Locale.getDefault();
		StringBuilder formatter = new StringBuilder();
		Iterator<Record.Entry> entries = record.entries().iterator();
		List<String> formattedValues = new ArrayList<>(record.size());
		while (entries.hasNext()) {
			Record.Entry entry = entries.next();
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			formatter.append(formatterFor(paddings.get(entry.getKey())));
			entry.getValue().print(printWriter, locale);
			String formattedValue = writer.toString();
			formattedValues.add(formattedValue);
		}
		String row = String.format(formatter.toString(), formattedValues.toArray());
		out.send(Records.singleton(Keys.TEXT, Values.ofText(row)));
	}

	private String formatterFor(int length) {
		return String.format("%%-%ds", length);
	}

	private void sendHeader(Map<Key, Integer> paddings, Record record, OutputChannel out) {
		String format = record.keys()
				.map(paddings::get)
				.map(this::formatterFor)
				.collect(Collectors.joining());
		String header = String.format(format, record.keys().map(Key::name).toArray());
		out.send(Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText(header), Ansi.Style.FG_MAGENTA)));
	}

}
