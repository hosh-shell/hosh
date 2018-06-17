package org.hosh.modules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Values;

public class FileSystemModule implements Module {

	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("cd", new ChangeDirectory());
		commandRegistry.registerCommand("ls", new ListFiles());
		commandRegistry.registerCommand("cwd", new CurrentWorkingDirectory());
		commandRegistry.registerCommand("cat", new Cat());
	}

	public static class ListFiles implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(state.getCwd())) {
				for (Path path : stream) {
					Record entry = Record.of("name", Values.ofPath(path.getFileName()));
					if (Files.isRegularFile(path)) {
						long size = Files.size(path);
						entry = entry.add("size", Values.ofHumanizedSize(size));
					}
					out.send(entry);
				}
			} catch (IOException e) {
				err.send(Record.of("exception", Values.ofText(e.getMessage())));
			}
		}
	}

	public static class CurrentWorkingDirectory implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of("error", Values.ofText("expecting no parameters")));
				return;
			}
			out.send(Record.of("cwd", Values.ofPath(state.getCwd())));
		}
	}

	public static class ChangeDirectory implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {

			switch (args.size()) {
				case 0:
					err.send(Record.of("error", Values.ofText("missing path argument")));
					break;
				case 1:
					Path arg = Paths.get(args.get(0));
					final Path newCwd;
					if (arg.isAbsolute()) {
						newCwd = arg;
					} else {
						newCwd = Paths.get(state.getCwd().toString(), args.get(0));
					}
					if (Files.isDirectory(newCwd)) {
						state.setCwd(newCwd);
					} else {
						err.send(Record.of("error", Values.ofText("not a directory")));
					}
					break;
				default:
					err.send(Record.of("error", Values.ofText("expecting one path argument")));
					break;
			}
		}

	}

	public static class Cat implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			switch (args.size()) {
				case 1:
					Path path = Paths.get(args.get(0));
					if (!path.isAbsolute()) {
						path = Paths.get(state.getCwd().toString(), args.get(0));
					}
					if (Files.isRegularFile(path)) {
						readLines(path, out, err);
					} else {
						err.send(Record.of("error", Values.ofText("not readable file")));
					}
					break;
				default:
					err.send(Record.of("error", Values.ofText("expecting one path argument")));
					break;
			}
		}

		private void readLines(Path path, Channel out, Channel err) {
			try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
				lines.forEach(line -> {
					out.send(Record.of("line", Values.ofText(line)));
				});
			} catch (IOException e) {
				err.send(Record.of("exception", Values.ofText(e.getMessage())));
			}
		}

	}
}
