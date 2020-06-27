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
package hosh.runtime;

import hosh.spi.Ansi;
import hosh.spi.InputChannel;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AutoTableChannel implements OutputChannel {

	private static final int COLUMN_PADDING = 2;
	private static final int OVERFLOW = 10_000;

	private final Logger logger = LoggerFactory.forEnclosingClass();
	private final OutputChannel outputChannel;
	private final List<Record> records;
	private volatile boolean overflow;
	private volatile boolean ended;

	public AutoTableChannel(OutputChannel outputChannel) {
		this.outputChannel = outputChannel;
		this.records = new ArrayList<>();
		this.overflow = false;
		this.ended = false;
	}

	@Override
	public void send(Record record) {
		if (ended) {
			// avoiding concurrent modification of "records" during when end is called before overflow
			return;
		}
		if (overflow) {
			// overflow already happened: short circuit
			outputChannel.send(record);
			return;
		}
		if (records.size() > OVERFLOW) {
			logger.info("autotable: overflow after " + OVERFLOW);
			overflow = true;
			// flush and clear our buffer
			records.stream().forEach(outputChannel::send);
			return;
		}
		records.add(record);
	}

	public void end() {
		ended = true;
		logger.info("autotable: end with overflow=" + overflow);
		if (!overflow) {
			Map<Key, Integer> paddings = calculatePaddings(records);
			outputTable(outputChannel, records, paddings);
		}
		records.clear();
		overflow = false;
		ended = false;
	}

	private void outputTable(OutputChannel out, List<Record> records, Map<Key, Integer> paddings) {
		boolean headerSent = false;
		for (Record record : records) {
			if (!headerSent) {
				sendHeader(paddings, record.keys(), out);
				headerSent = true;
			}
			sendRow(paddings, record, out);
		}
	}

	private Map<Key, Integer> calculatePaddings(List<Record> records) {
		Map<Key, Integer> maxLengthPerColumn = new HashMap<>();
		for (Record record : records) {
			for (Record.Entry entry : record.entries()) {
				String formattedValue = valueAsString(entry.getValue());
				int valueLength = lengthFor(formattedValue);
				maxLengthPerColumn.compute(entry.getKey(), (k, v) -> v == null ? Math.max(k.name().length(), valueLength) : Math.max(v, valueLength));
			}
		}
		Map<Key, Integer> result = new HashMap<>();
		for (var kv : maxLengthPerColumn.entrySet()) {
			result.put(kv.getKey(), kv.getValue() + COLUMN_PADDING);
		}
		logger.log(Level.FINE, "paddings = " + maxLengthPerColumn);
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

	// consuming all records here... it could be a problem for big output
	private List<Record> accumulate(InputChannel in) {
		List<Record> records = new LinkedList<>();
		for (Record record : InputChannel.iterate(in)) {
			records.add(record);
		}
		return records;
	}

	private void sendRow(Map<Key, Integer> paddings, Record record, OutputChannel out) {
		Locale locale = Locale.getDefault();
		StringBuilder formatter = new StringBuilder();
		Collection<Record.Entry> entries = record.entries();
		List<String> formattedValues = new ArrayList<>(entries.size());
		for (Record.Entry entry : entries) {
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

	private void sendHeader(Map<Key, Integer> paddings, Collection<Key> keys, OutputChannel out) {
		String format = keys.stream()
			.map(paddings::get)
			.map(this::formatterFor)
			.collect(Collectors.joining());
		String header = String.format(format, keys.stream().map(Key::name).toArray());
		logger.log(Level.FINE, "format = " + format);
		out.send(Records.singleton(Keys.TEXT, Values.withStyle(Values.ofText(header), Ansi.Style.FG_MAGENTA)));
	}


}
