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
package hosh.modules.filesystem;

import hosh.doc.Bug;
import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Experimental;
import hosh.doc.Todo;
import hosh.spi.*;
import hosh.spi.Module;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileSystemModule implements Module {

	@Override
	public void initialize(CommandRegistry registry) {
		registry.registerCommand("ls", ListFiles::new);
		registry.registerCommand("cwd", CurrentWorkingDirectory::new);
		registry.registerCommand("cd", ChangeDirectory::new);
		registry.registerCommand("lines", Lines::new);
		registry.registerCommand("walk", Walk::new);
		registry.registerCommand("glob", Glob::new);
		registry.registerCommand("cp", Copy::new);
		registry.registerCommand("mv", Move::new);
		registry.registerCommand("rm", Remove::new);
		registry.registerCommand("partitions", Partitions::new);
		registry.registerCommand("probe", Probe::new);
		registry.registerCommand("symlink", Symlink::new);
		registry.registerCommand("hardlink", Hardlink::new);
		registry.registerCommand("resolve", Resolve::new);
		registry.registerCommand("watch", Watch::new);
		registry.registerCommand("withLock", WithLock::new);
	}

	@Description("list files")
	@Examples({
			@Example(command = "ls", description = "list current directory"),
			@Example(command = "ls /tmp", description = "list specified absolute directory"),
			@Example(command = "ls directory", description = "list relative directory")
	})
	@Todo(description = "add support for uid, gid, permissions")
	public static class ListFiles implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() > 1) {
				err.send(Errors.usage("ls [directory]"));
				return ExitStatus.error();
			}
			final Path cwd = state.getCwd();
			final Path dir;
			if (args.size() == 1) {
				dir = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			} else {
				dir = cwd;
			}
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(followSymlinksRecursively(dir))) {
				for (Path path : stream) {
					BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
					Value size = attributes.isRegularFile() ? Values.ofSize(attributes.size()) : Values.none();
					Record entry = Records.builder()
							.entry(Keys.PATH, Values.ofPath(path.getFileName()))
							.entry(Keys.SIZE, size)
							.entry(Keys.CREATED, Values.ofInstant(attributes.creationTime().toInstant()))
							.entry(Keys.MODIFIED, Values.ofInstant(attributes.lastModifiedTime().toInstant()))
							.entry(Keys.ACCESSED, Values.ofInstant(attributes.lastAccessTime().toInstant()))
							.build();
					out.send(entry);
				}
				return ExitStatus.success();
			} catch (NotDirectoryException e) {
				err.send(Errors.message("not a directory: %s", e.getMessage()));
				return ExitStatus.error();
			} catch (AccessDeniedException e) {
				err.send(Errors.message("access denied: %s", e.getMessage()));
				return ExitStatus.error();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("output current working directory")
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
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (!args.isEmpty()) {
				err.send(Errors.usage("cwd"));
				return ExitStatus.error();
			}
			out.send(Records.singleton(Keys.PATH, Values.ofPath(state.getCwd())));
			return ExitStatus.success();
		}
	}

	@Description("set new current working directory")
	@Examples({
			@Example(command = "cd dir", description = "change current working directory to 'dir'"),
			@Example(command = "cd /tmp", description = "change current working directory to '/tmp'"),
	})
	public static class ChangeDirectory implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("cd directory"));
				return ExitStatus.error();
			}
			Path newCwd = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			if (!Files.isDirectory(newCwd)) {
				err.send(Errors.message("not a directory"));
				return ExitStatus.error();
			}
			state.setCwd(newCwd);
			return ExitStatus.success();
		}
	}

	@Description("output file line by line")
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
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("lines file"));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			if (!Files.isRegularFile(source)) {
				err.send(Errors.message("not readable file"));
				return ExitStatus.error();
			}
			try (Stream<String> lines = Files.lines(source, StandardCharsets.UTF_8)) {
				lines.forEach(line -> out.send(Records.singleton(Keys.TEXT, Values.ofText(line))));
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("walk directory recursively")
	@Examples({
			@Example(command = "walk .", description = "recursively output all paths in '.'"),
			@Example(command = "walk /tmp", description = "recursively output all paths in '/tmp'"),
	})
	public static class Walk implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("walk directory"));
				return ExitStatus.error();
			}
			try {
				Path target = followSymlinksRecursively(resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0))));
				if (!Files.exists(target)) {
					err.send(Errors.message("not found"));
					return ExitStatus.error();
				}
				if (!Files.isDirectory(target)) {
					err.send(Errors.message("not a directory"));
					return ExitStatus.error();
				}
				Files.walkFileTree(target, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new VisitCallback(out, err));
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private static class VisitCallback implements FileVisitor<Path> {

			private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

			private final OutputChannel out;
			private final OutputChannel err;

			public VisitCallback(OutputChannel out, OutputChannel err) {
				this.out = out;
				this.err = err;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				out.send(Records
						.builder()
						.entry(Keys.PATH, Values.ofPath(file))
						.entry(Keys.SIZE, Values.ofSize(attrs.size()))
						.build());
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				LOGGER.log(Level.SEVERE, exc, () -> String.format("error while visiting: %s", file));
				err.send(Errors.message(exc));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
				return FileVisitResult.CONTINUE;
			}
		}
	}

	@Todo(description = "allows to select path key")
	@Description("glob pattern matching")
	@Examples({
			@Example(command = "walk . | glob '*.java'", description = "recursively find all 'java' files")
	})
	public static class Glob implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("glob pattern"));
				return ExitStatus.error();
			}
			String pattern = args.get(0);
			PathMatcher pathMatcher = state.getCwd().getFileSystem().getPathMatcher("glob:" + pattern);
			for (Record record : InputChannel.iterate(in)) {
				record.value(Keys.PATH)
						.flatMap(v -> v.unwrap(Path.class))
						.map(Path::getFileName)
						.filter(pathMatcher::matches)
						.ifPresent(p -> out.send(record)); // side effect
			}
			return ExitStatus.success();
		}
	}

	@Todo(description = "test 'cp file directory'")
	@Description("copy file")
	@Examples({
			@Example(command = "cp source.txt target.txt", description = "copy file using current working directory"),
			@Example(command = "cp /tmp/source.txt /tmp/target.txt", description = "copy file by using absolute path"),
	})
	public static class Copy implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Errors.usage("cp file file"));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			Path target = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(1)));
			try {
				Files.copy(source, target);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Todo(description = "test 'mv file directory'")
	@Description("move file")
	@Examples({
			@Example(command = "mv source.txt target.txt", description = "move file using current working directory"),
			@Example(command = "mv /tmp/source.txt /tmp/target.txt", description = "move file by using absolute path"),
	})
	public static class Move implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Errors.usage("mv file file"));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			Path target = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(1)));
			try {
				Files.move(source, target);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("remove file")
	@Examples({
			@Example(command = "rm target.txt", description = "remove file using current working directory"),
			@Example(command = "rm /tmp/target.txt", description = "remove file by using absolute path"),
	})
	public static class Remove implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("rm file"));
				return ExitStatus.error();
			}
			Path target = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			try {
				Files.delete(target);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("show partitions information like df -h")
	@Examples({
			@Example(command = "partitions", description = "show all partitions"),
	})
	public static class Partitions implements Command {

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (!args.isEmpty()) {
				err.send(Errors.usage("partitions"));
				return ExitStatus.error();
			}
			for (FileStore store : FileSystems.getDefault().getFileStores()) {
				try {
					out.send(Records
							.builder()
							.entry(Keys.of("name"), Values.ofText(store.name()))
							.entry(Keys.of("type"), Values.ofText(store.type()))
							.entry(Keys.of("total"), Values.ofSize(store.getTotalSpace()))
							.entry(Keys.of("used"), Values.ofSize(store.getTotalSpace() - store.getUnallocatedSpace()))
							.entry(Keys.of("free"), Values.ofSize(store.getUsableSpace()))
							.entry(Keys.of("blocksize"), Values.ofSize(store.getBlockSize()))
							.entry(Keys.of("readonly"), Values.ofText(store.isReadOnly() ? "yes" : "no"))
							.build());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			return ExitStatus.success();
		}
	}

	@Experimental(description = "usefulness of this command is quite limited right now")
	@Description("detect content type of a file")
	@Examples({
			@Example(command = "probe file", description = "attempt to detect content type"),
	})
	public static class Probe implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("probe file"));
				return ExitStatus.error();
			}
			Path file = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			try {
				String contentType = Files.probeContentType(file);
				if (contentType == null) {
					err.send(Errors.message("content type cannot be determined"));
					return ExitStatus.error();
				}
				out.send(Records.singleton(Keys.of("contenttype"), Values.ofText(contentType)));
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("create symlink")
	@Examples({
			@Example(command = "symlink source target", description = "create symlink"),
	})
	public static class Symlink implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Errors.usage("symlink source target"));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			Path target = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(1)));
			try {
				Files.createSymbolicLink(target, source);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("create hardlink")
	@Examples({
			@Example(command = "hardlink source target", description = "create hardlink"),
	})
	public static class Hardlink implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 2) {
				err.send(Errors.usage("hardlink source target"));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			Path target = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(1)));
			try {
				Files.createLink(target, source);
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Todo(description = "add tests for regular files and directories")
	@Description("resolve to canonical absolute path")
	@Examples({
			@Example(command = "resolve ./symlink", description = "resolve symlink to the absolute path"),
	})
	public static class Resolve implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("resolve file"));
				return ExitStatus.error();
			}
			try {
				Path unresolved = Path.of(args.get(0));
				Path partiallyResolved = resolveAsAbsolutePath(state.getCwd(), unresolved);
				Path resolved = partiallyResolved.toRealPath();
				out.send(Records.singleton(Keys.PATH, Values.ofPath(resolved)));
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	@Description("watch for filesystem change in the given directory")
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
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (!args.isEmpty()) {
				err.send(Errors.usage("watch directory"));
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

		private void withService(WatchService watchService, OutputChannel out) throws InterruptedException {
			while (true) {
				LOGGER.info("waiting for events");
				WatchKey key = watchService.take();
				handle(out, key);
				if (!key.reset()) {
					break;
				}
			}
		}

		private void handle(OutputChannel out, WatchKey key) {
			for (var event : key.pollEvents()) {
				if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
					LOGGER.warning("got overflow");
					continue;
				}
				@SuppressWarnings("unchecked")
				WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
				out.send(Records.builder()
						.entry(Keys.of("type"), Values.ofText(event.kind().name().replace("ENTRY_", "")))
						.entry(Keys.PATH, Values.ofPath(pathEvent.context()))
						.build());
			}
		}
	}

	@Description("execute inner block after successfully locking file")
	@Examples({
			@Example(command = "withLock file.lock { echo 'critical section' }", description = "echo only if lock has been acquired")
	})
	public static class WithLock implements CommandDecorator, StateAware {

		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();
		private static final Duration BUSY_WAIT = Duration.ofMillis(200);

		private State state;

		private CommandNested commandNested;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void setCommandNested(CommandNested commandNested) {
			this.commandNested = commandNested;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				err.send(Errors.usage("withLock file { ... }"));
				return ExitStatus.error();
			}
			Path path = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			RandomAccessFile randomAccessFile = null;
			try {
				randomAccessFile = new RandomAccessFile(path.toFile(), "rw");
				busyWaitForResource(err, randomAccessFile);
				return commandNested.run();
			} catch (IOException e) {
				err.send(Errors.message(e));
				return ExitStatus.error();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return ExitStatus.error();
			} finally {
				try {
					if (randomAccessFile != null) {
						randomAccessFile.close();
						Files.delete(path);
					}
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, "caught exception during cleanup", e);
				}
			}
		}

		@SuppressWarnings("BusyWait")
		private void busyWaitForResource(OutputChannel err, RandomAccessFile randomAccessFile) throws IOException, InterruptedException {
			while (randomAccessFile.getChannel().tryLock() == null) {
				Thread.sleep(BUSY_WAIT.toMillis());
				err.send(Errors.message("failed to acquire lock, retry in %sms", BUSY_WAIT.toMillis()));
			}
		}

	}

	private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

	private static Path followSymlinksRecursively(Path path) throws IOException {
		if (Files.isSymbolicLink(path)) {
			Path target = Files.readSymbolicLink(path);
			Path resolvedPath = path.getParent().resolve(target);
			LOGGER.fine(() -> String.format("  resolving symlink '%s' to '%s'", path, resolvedPath));
			return followSymlinksRecursively(resolvedPath);
		} else {
			return path;
		}
	}

	private static Path resolveAsAbsolutePath(Path cwd, Path file) {
		if (file.isAbsolute()) {
			return normalized(file);
		}
		return normalized(cwd.resolve(file));
	}

	private static Path normalized(Path path) {
		return path.normalize().toAbsolutePath();
	}
}
