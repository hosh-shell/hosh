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

import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.spi.CommandRegistry;
import hosh.spi.OutputChannel;
import hosh.spi.Command;
import hosh.spi.ExitStatus;
import hosh.spi.HistoryAware;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.Module;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.Values;
import org.jline.reader.History;

import java.util.List;

public class HistoryModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand("history", ListHistory.class);
	}

	@Description("display the history with timestamp, index and text")
	@Examples({
		@Example(command = "history", description = "show all history")
	})
	public static class ListHistory implements Command, HistoryAware {

		private History history;

		@Override
		public void setHistory(History history) {
			this.history = history;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 0) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("no arguments expected")));
				return ExitStatus.error();
			}

			for (var entry : history) {
				Record record = Records.builder()
					                .entry(Keys.TIMESTAMP, Values.ofInstant(entry.time()))
					                .entry(Keys.INDEX, Values.ofNumeric(entry.index()))
					                .entry(Keys.TEXT, Values.ofText(entry.line()))
					                .build();
				out.send(record);
			}

			return ExitStatus.success();
		}
	}
}
