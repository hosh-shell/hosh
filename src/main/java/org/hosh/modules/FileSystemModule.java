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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.hosh.doc.Bug;
import org.hosh.doc.Example;
import org.hosh.doc.Examples;
import org.hosh.doc.Help;
import org.hosh.doc.Todo;
import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.ExitStatus;
import org.hosh.spi.Keys;
import org.hosh.spi.LoggerFactory;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;
import org.hosh.spi.Value;
import org.hosh.spi.Values;

public class FileSystemModule implements Module {

	@Override
	public void onStartup(CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("cd", ChangeDirectory.class);
		commandRegistry.registerCommand("ls", ListFiles.class);
		commandRegistry.registerCommand("cwd", CurrentWorkingDirectory.class);
		commandRegistry.registerCommand("lines", Lines.class);
		commandRegistry.registerCommand("find", Find.class);
		commandRegistry.registerCommand("watch", Watch.class);
		commandRegistry.registerCommand("cp", Copy.class);
		commandRegistry.registerCommand("mv", Move.class);
		commandRegistry.registerCommand("rm", Remove.class);
	}

	@Help(description = "list files")
	@Examples({
			@Example(command = "ls", description = "list current directory"),
			@Example(command = "ls /tmp", description = "list specified absolute directory"),
			@Example(command = "ls directory", description = "list relative directory")
	})
	public static class ListFiles implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() > 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expected at most 1 argument")));
				return ExitStatus.error();
			}
			final Path cwd = state.getCwd();
			final Path dir;
			if (args.size() == 1) {
				Path arg = Paths.get(args.get(0));
				if (arg.isAbsolute()) {
					dir = arg;
				} else {
					dir = cwd.resolve(arg).toAbsolutePath().normalize();
				}
			} else {
				dir = cwd;
			}
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(followSymlinksRecursively(dir))) {
				for (Path path : stream) {
					Value size = Files.isRegularFile(path) ? Values.ofHumanizedSize(Files.size(path)) : Values.none();
					Record entry = Record.builder()
							.entry(Keys.PATH, Values.ofPath(path.getFileName()))
							.entry(Keys.SIZE, size)
							.build();
					out.send(entry);
				}
				return ExitStatus.success();
			} catch (NotDirectoryException e) {
				err.send(Record.of(Keys.ERROR, Values.ofText("not a directory: " + e.getMessage())));
				return ExitStatus.error();
			} catch (AccessDeniedException e) {
				err.send(Record.of(Keys.ERROR, Values.ofText("access denied: " + e.getMessage())));
				return ExitStatus.error();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Help(description = "output current working directory")
	@Examples({
			@Example(command = "cwd", description = "current working directory"),
	})
	public static class CurrentWorkingDirectory implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting no arguments")));
				return ExitStatus.error();
			}
			out.send(Record.of(Keys.PATH, Values.ofPath(state.getCwd())));
			return ExitStatus.success();
		}
	}

	@Help(description = "set new current working directory")
	@Examples({
			@Example(command = "cd local_dir", description = "change current working directory to 'local_dir'"),
			@Example(command = "cd /tmp", description = "change current working directory to '/tmp'"),
	})
	public static class ChangeDirectory implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting one argument (directory)")));
				return ExitStatus.error();
			}
			Path arg = Paths.get(args.get(0));
			final Path newCwd;
			if (arg.isAbsolute()) {
				newCwd = arg;
			} else {
				newCwd = Paths.get(state.getCwd().toString(), args.get(0));
			}
			if (!Files.isDirectory(newCwd)) {
				err.send(Record.of(Keys.ERROR, Values.ofText("not a directory")));
				return ExitStatus.error();
			}
			state.setCwd(newCwd);
			return ExitStatus.success();
		}
	}

	@Help(description = "output file line by line")
	@Examples({
			@Example(command = "lines file.txt", description = "output all lines of 'file.txt'"),
	})
	public static class Lines implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting one path argument")));
				return ExitStatus.error();
			}
			Path path = Paths.get(args.get(0));
			if (!path.isAbsolute()) {
				path = Paths.get(state.getCwd().toString(), args.get(0));
			}
			if (!Files.isRegularFile(path)) {
				err.send(Record.of(Keys.ERROR, Values.ofText("not readable file")));
				return ExitStatus.error();
			}
			try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
				lines.forEach(line -> out.send(Record.of(Keys.TEXT, Values.ofText(line))));
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Help(description = "walk directory recursively")
	@Examples({
			@Example(command = "find .", description = "recursively output all paths in '.'"),
			@Example(command = "find /tmp", description = "recursively output all paths in '/tmp'"),
	})
	public static class Find implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting one argument")));
				return ExitStatus.error();
			}
			final Path arg = Path.of(args.get(0));
			final Path start;
			if (arg.isAbsolute()) {
				start = arg;
			} else {
				start = state.getCwd().resolve(arg).normalize().toAbsolutePath();
			}
			try (Stream<Path> stream = Files.walk(followSymlinksRecursively(start))) {
				stream.forEach(path -> {
					Record result = Record.of(Keys.PATH, Values.ofPath(path.toAbsolutePath()));
					out.send(result);
				});
				return ExitStatus.success();
			} catch (NoSuchFileException e) {
				err.send(Record.of(Keys.ERROR, Values.ofText("path does not exist: " + e.getFile())));
				return ExitStatus.error();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Todo(description = "make use of Files.copy() return value?")
	@Help(description = "copy file")
	@Examples({
			@Example(command = "cp source.txt target.txt", description = "copy file using current working directory"),
			@Example(command = "cp /tmp/source.txt /tmp/target.txt", description = "copy file by using absolute path"), })
	public static class Copy implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Record.of(Keys.ERROR, Values.ofText("usage: source target")));
				return ExitStatus.error();
			}
			Path source = Path.of(args.get(0));
			if (!source.isAbsolute()) {
				source = state.getCwd().resolve(source).normalize().toAbsolutePath();
			}
			Path target = Path.of(args.get(1));
			if (!target.isAbsolute()) {
				target = state.getCwd().resolve(target).normalize().toAbsolutePath();
			}
			try {
				Files.copy(source, target);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Todo(description = "make use of Files.move() return value?")
	@Help(description = "move file")
	@Examples({
			@Example(command = "mv source.txt target.txt", description = "move file using current working directory"),
			@Example(command = "mv /tmp/source.txt /tmp/target.txt", description = "move file by using absolute path"), })
	public static class Move implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 2) {
				err.send(Record.of(Keys.ERROR, Values.ofText("usage: source target")));
				return ExitStatus.error();
			}
			Path source = Path.of(args.get(0));
			if (!source.isAbsolute()) {
				source = state.getCwd().resolve(source).normalize().toAbsolutePath();
			}
			Path target = Path.of(args.get(1));
			if (!target.isAbsolute()) {
				target = state.getCwd().resolve(target).normalize().toAbsolutePath();
			}
			try {
				Files.move(source, target);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Todo(description = "make use of Files.move() return value?")
	@Help(description = "remove file")
	@Examples({
			@Example(command = "rm target.txt", description = "remove file using current working directory"),
			@Example(command = "rm /tmp/target.txt", description = "remove file by using absolute path"), })
	public static class Remove implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (args.size() != 1) {
				err.send(Record.of(Keys.ERROR, Values.ofText("usage: rm target")));
				return ExitStatus.error();
			}
			Path target = Path.of(args.get(0));
			if (!target.isAbsolute()) {
				target = state.getCwd().resolve(target).normalize().toAbsolutePath();
			}
			try {
				Files.delete(target);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Help(description = "watch for filesystem change in the given path")
	@Examples({
			@Example(command = "watch", description = "output records with type='CREATE|MODIFY|DELETE' and path in current working directory")
	})
	@Bug(description = "should be recursive by default", issue = "https://github.com/dfa1/hosh/issues/94")
	public static class Watch implements Command, StateAware {

		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, Channel in, Channel out, Channel err) {
			if (!args.isEmpty()) {
				err.send(Record.of(Keys.ERROR, Values.ofText("expecting no arguments")));
				return ExitStatus.error();
			}
			Path dir = state.getCwd();
			WatchEvent.Kind<?>[] events = {
					StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_MODIFY
			};
			try (WatchService watchService = dir.getFileSystem().newWatchService()) {
				dir.register(watchService, events);
				withService(watchService, out);
				return ExitStatus.success();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return ExitStatus.error();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private void withService(WatchService watchService, Channel out) throws InterruptedException {
			for (;;) {
				LOGGER.info("waiting for events");
				WatchKey key = watchService.take();
				for (var event : key.pollEvents()) {
					if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
						LOGGER.warning("got overflow");
						continue;
					}
					@SuppressWarnings("unchecked")
					WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
					out.send(Record.builder()
							.entry(Keys.of("type"), Values.ofText(event.kind().name().replace("ENTRY_", "")))
							.entry(Keys.PATH, Values.ofPath(pathEvent.context()))
							.build());
				}
				if (!key.reset()) {
					break;
				}
			}
		}
	}

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private static Path followSymlinksRecursively(Path path) throws IOException {
		if (Files.isSymbolicLink(path)) {
			Path target = Files.readSymbolicLink(path);
			Path resolvedPath = path.getParent().resolve(target);
			LOGGER.fine(() -> String.format("'%s' -> '%s'", path, resolvedPath));
			return followSymlinksRecursively(resolvedPath);
		} else {
			return path;
		}
	}
}
