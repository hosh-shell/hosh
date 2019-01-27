package org.hosh.modules;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.hosh.doc.Experimental;
import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.Value;
import org.hosh.spi.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextModule implements Module {
	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("schema", new Schema());
		commandRegistry.registerCommand("filter", new Filter());
		commandRegistry.registerCommand("enumerate", new Enumerate());
		commandRegistry.registerCommand("sort", new Sort());
		commandRegistry.registerCommand("take", new Take());
		commandRegistry.registerCommand("drop", new Drop());
		commandRegistry.registerCommand("rand", new Rand());
		commandRegistry.registerCommand("count", new Count());
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
				Value value = record.value(key);
				if (value != null && value.matches(regex)) {
					out.send(record);
				}
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
			int i = 1;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				out.send(record.prepend("index", Values.ofText(Integer.toString(i++))));
			}
		}
	}

	public static class Sort implements Command {
		private final Logger logger = LoggerFactory.getLogger(getClass());

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of("error", Values.ofText("expected 1 parameters")));
				return ExitStatus.error();
			}
			List<Record> records = new ArrayList<>();
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					break;
				}
				Record record = incoming.get();
				records.add(record);
			}
			String key = args.get(0);
			var comparator = Comparator.<Record, Value>comparing((r) -> r.value(key), Comparator.nullsFirst(Comparator.naturalOrder()));
			logger.debug("before sorting {}", records);
			records.sort(comparator);
			logger.debug("after sorting {}", records);
			for (Record record : records) {
				out.send(record);
			}
			return ExitStatus.success();
		}
	}

	@Todo(description = "error handling (e.g. non-integer and negative value)")
	public static class Take implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of("error", Values.ofText("expected 1 parameters")));
				return ExitStatus.error();
			}
			int take = Integer.parseInt(args.get(0));
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				if (take > 0) {
					out.send(record);
					take--;
				} else {
					in.requestStop();
					break;
				}
			}
			return ExitStatus.success();
		}
	}

	@Todo(description = "error handling (e.g. non-integer and negative value)")
	public static class Drop implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of("error", Values.ofText("expected 1 parameters")));
				return ExitStatus.error();
			}
			int drop = Integer.parseInt(args.get(0));
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					return ExitStatus.success();
				}
				Record record = incoming.get();
				if (drop > 0) {
					drop--;
				} else {
					out.send(record);
				}
			}
		}
	}

	@Experimental(description = "extends with seed, bounds, doubles, booleans, etc")
	public static class Rand implements Command {
		private final Logger logger = LoggerFactory.getLogger(getClass());

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of("error", Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			SecureRandom secureRandom;
			try {
				secureRandom = SecureRandom.getInstanceStrong();
			} catch (NoSuchAlgorithmException e) {
				logger.error("failed to get SecureRandom instance", e);
				err.send(Record.of("error", Values.ofText(e.getMessage())));
				return ExitStatus.error();
			}
			while (true) {
				int nextInt = secureRandom.nextInt();
				Record of = Record.of("value", Values.ofText(String.valueOf(nextInt)));
				boolean halted = out.trySend(of);
				if (halted) {
					break;
				}
			}
			return ExitStatus.success();
		}
	}

	public static class Count implements Command {
		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 0) {
				err.send(Record.of("error", Values.ofText("expected 0 parameters")));
				return ExitStatus.error();
			}
			int count = 0;
			while (true) {
				Optional<Record> incoming = in.recv();
				if (incoming.isEmpty()) {
					out.send(Record.of("cound", Values.ofText("" + count)));
					return ExitStatus.success();
				} else {
					count++;
				}
			}
		}
	}
}
