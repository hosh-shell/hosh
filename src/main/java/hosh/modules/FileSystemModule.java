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
package hosh.modules;

import hosh.doc.Bug;
import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.doc.Experimental;
import hosh.doc.Todo;
import hosh.spi.Ansi;
import hosh.spi.Command;
import hosh.spi.CommandRegistry;
import hosh.spi.CommandWrapper;
import hosh.spi.ExitStatus;
import hosh.spi.InputChannel;
import hosh.spi.Keys;
import hosh.spi.LoggerFactory;
import hosh.spi.Module;
import hosh.spi.OutputChannel;
import hosh.spi.Record;
import hosh.spi.Records;
import hosh.spi.State;
import hosh.spi.StateAware;
import hosh.spi.Value;
import hosh.spi.Values;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
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
	@Todo(description = "add support for mtime, atime, ctime, uid, gid, etc")
	public static class ListFiles implements Command, StateAware {

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public ExitStatus run(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() > 1) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expected at most 1 argument")));
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
						               .entry(Keys.PATH, Values.ofStyledPath(path.getFileName(), colorFor(attributes)))
						               .entry(Keys.SIZE, size)
						               .build();
					out.send(entry);
				}
				return ExitStatus.success();
			} catch (NotDirectoryException e) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("not a directory: " + e.getMessage())));
				return ExitStatus.error();
			} catch (AccessDeniedException e) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("access denied: " + e.getMessage())));
				return ExitStatus.error();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private Ansi.Style colorFor(BasicFileAttributes attributes) {
			if (attributes.isDirectory()) {
				return Ansi.Style.FG_CYAN;
			}
			if (attributes.isSymbolicLink()) {
				return Ansi.Style.FG_MAGENTA;
			}
			if (attributes.isOther()) {
				return Ansi.Style.FG_YELLOW;
			}
			return Ansi.Style.NONE;
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting no arguments")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument (directory)")));
				return ExitStatus.error();
			}
			Path newCwd = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			if (!Files.isDirectory(newCwd)) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("not a directory")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting one path argument")));
				return ExitStatus.error();
			}
			Path source = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			if (!Files.isRegularFile(source)) {
				err.send(Records.singleton(Keys.ERROR, Values.ofText("not readable file")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument")));
				return ExitStatus.error();
			}
			try {
				Path target = followSymlinksRecursively(resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0))));
				if (!Files.exists(target)) {
					err.send(Records.singleton(Keys.ERROR, Values.ofText("not found")));
					return ExitStatus.error();
				}
				if (!Files.isDirectory(target)) {
					err.send(Records.singleton(Keys.ERROR, Values.ofText("not a directory")));
					return ExitStatus.error();
				}
				Files.walkFileTree(target, new VisitCallback(out));
				return ExitStatus.success();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private static class VisitCallback implements FileVisitor<Path> {

			private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

			private final OutputChannel out;

			public VisitCallback(OutputChannel out) {
				this.out = out;
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
				LOGGER.log(Level.SEVERE, "error while visiting: " + file, exc);
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting one argument")));
				return ExitStatus.error();
			}
			String pattern = args.get(0);
			PathMatcher pathMatcher = state.getCwd().getFileSystem().getPathMatcher("glob:" + pattern);
			for (Record record : InputChannel.iterate(in)) {
				record.value(Keys.PATH)
					.flatMap(v -> v.unwrap(Path.class))
					.map(Path::getFileName)
					.filter(pathMatcher::matches)
					.ifPresent(p -> {
						out.send(record);
					}
				);
			}
			return ExitStatus.success();
		}
	}

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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: source target")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: source target")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: rm target")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: partitions")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: probe file")));
				return ExitStatus.error();
			}
			Path file = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
			try {
				String contentType = Files.probeContentType(file);
				if (contentType == null) {
					err.send(Records.singleton(Keys.ERROR, Values.ofText("content type cannot be determined")));
					return ExitStatus.success();
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: symlink source target")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: hardlink source target")));
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("usage: resolve file")));
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

	@Description("watch for filesystem change in the given path")
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
				err.send(Records.singleton(Keys.ERROR, Values.ofText("expecting no arguments")));
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

	@Experimental(description = "naive and ugly implementation (e.g. sleep(200) and output on err)")
	@Description("execute inner block after successfully locking file")
	@Examples({
		@Example(command = "withLock file.lock { echo 'critical section' }", description = "echo only if lock has been acquired")
	})
	public static class WithLock implements CommandWrapper<WithLock.LockResource>, StateAware {

		private static final Logger LOGGER = LoggerFactory.forEnclosingClass();

		private State state;

		@Override
		public void setState(State state) {
			this.state = state;
		}

		@Override
		public LockResource before(List<String> args, InputChannel in, OutputChannel out, OutputChannel err) {
			if (args.size() != 1) {
				throw new IllegalArgumentException("expecting file name");
			}
			try {
				Path path = resolveAsAbsolutePath(state.getCwd(), Path.of(args.get(0)));
				RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "rw");
				while (randomAccessFile.getChannel().tryLock() == null) {
					Thread.sleep(200);
					err.send(Records.singleton(Keys.ERROR, Values.ofText("failed to acquire lock")));
				}
				return new LockResource(randomAccessFile, path);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}
		}

		@Override
		public void after(LockResource resource, InputChannel in, OutputChannel out, OutputChannel err) {
			try {
				resource.randomAccessFile.close();
				Files.delete(resource.path);
			} catch (IOException e) {
				LOGGER.log(Level.INFO, "caught exception", e);
				err.send(Records.singleton(Keys.ERROR, Values.ofText(e.getMessage())));
			}
		}

		@Override
		public boolean retry(LockResource resource, InputChannel in, OutputChannel out, OutputChannel err) {
			return false;
		}

		public static class LockResource {

			private final RandomAccessFile randomAccessFile;

			private final Path path;

			public LockResource(RandomAccessFile randomAccessFile, Path path) {
				this.randomAccessFile = randomAccessFile;
				this.path = path;
			}

			public Path getPath() {
				return path;
			}

			public RandomAccessFile getRandomAccessFile() {
				return randomAccessFile;
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
