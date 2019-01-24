package org.hosh.modules;

import java.util.List;
import java.util.Optional;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.Values;

public class TextModule implements Module {
	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("schema", new Schema());
		commandRegistry.registerCommand("filter", new Filter());
		commandRegistry.registerCommand("enumerate", new Enumerate());
	}

	public static class Schema implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of("error", Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				out.send(Record.of("keys", Values.ofText(record.keys().toString())));
			}
		}
	}

	public static class Filter implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Record.of("error", Values.ofText("expected 2 parameters")));
				return ExitStatus.error();
			}
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				String key = args.get(0);
				String regex = args.get(1);
				Record record = incoming.get();
				record
						.value(key)
						.filter(v -> v.toString().matches(regex))
						.ifPresent(v -> {
							out.send(record);
						});
			}
		}
	}

	public static class Enumerate implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of("error", Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			int i = 0;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				out.send(record.add("index", Values.ofText(Integer.toString(i++))));
			}
		}
	}
}
