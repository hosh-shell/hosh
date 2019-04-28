/**
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
package org.hosh.modules;

import java.util.List;

import org.hosh.doc.Example;
import org.hosh.doc.Examples;
import org.hosh.doc.Help;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.Module;
import org.hosh.spi.Records;
import org.hosh.spi.TerminalAware;
import org.hosh.spi.Values;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

public class TerminalModule implements Module {

	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("dump", Dump.class);
		commandRegistry.registerCommand("clear", Clear.class);
		commandRegistry.registerCommand("bell", Bell.class);
	}

	@Help(description = "dump terminal info")
	@Examples({
			@Example(command = "dump", description = "output current terminal information (similar to stty -a)"),
	})
	public static class Dump implements Command, TerminalAware {

		private Terminal terminal;

		@Override
		public void setTerminal(Terminal terminal) {
			this.terminal = terminal;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			Attributes attributes = terminal.getAttributes();
			out.send(Records
					.builder()
					.entry(Keys.of("type"), Values.ofText(terminal.getType()))
					.entry(Keys.of("lflags"), Values.ofText(attributes.getLocalFlags().toString()))
					.entry(Keys.of("iflags"), Values.ofText(attributes.getInputFlags().toString()))
					.entry(Keys.of("oflags"), Values.ofText(attributes.getOutputFlags().toString()))
					.entry(Keys.of("cflags"), Values.ofText(attributes.getControlFlags().toString()))
					.entry(Keys.of("cchars"), Values.ofText(attributes.getControlChars().toString()))
					.build());
			return ExitStatus.success();
		}
	}

	@Help(description = "clear the terminal screen")
	@Examples({
			@Example(command = "clear", description = "clear terminal screen"),
	})
	public static class Clear implements Command, TerminalAware {

		private Terminal terminal;

		@Override
		public void setTerminal(Terminal terminal) {
			this.terminal = terminal;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("no arguments expected")));
				return ExitStatus.error();
			}
			terminal.puts(InfoCmp.Capability.clear_screen);
			terminal.flush();
			return ExitStatus.success();
		}
	}

	@Help(description = "ring/bell the terminal screen")
	@Examples({
			@Example(command = "bell", description = "send 'bell' command to the terminal"),
	})
	public static class Bell implements Command, TerminalAware {

		private Terminal terminal;

		@Override
		public void setTerminal(Terminal terminal) {
			this.terminal = terminal;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("no arguments expected")));
				return ExitStatus.error();
			}
			terminal.puts(InfoCmp.Capability.bell);
			terminal.flush();
			return ExitStatus.success();
		}
	}
}
