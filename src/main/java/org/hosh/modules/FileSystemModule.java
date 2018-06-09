package org.hosh.modules;

import org.hosh.spi.Channel;
import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
import org.hosh.spi.Record;
import org.hosh.spi.State;
import org.hosh.spi.StateAware;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileSystemModule implements Module {

	@Override
	public void onStartup(@Nonnull CommandRegistry commandRegistry) {
		commandRegistry.registerCommand("cd", ChangeDirectory.class);
		commandRegistry.registerCommand("ls", ListFiles.class);
		commandRegistry.registerCommand("cwd", CurrentWorkDirectory.class);
	}

	public static class ListFiles implements Command, StateAware {

		private State state;

		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(state.getCwd())) {
				for (Path path : stream) {
					String name = path.getFileName().toString();
					Record entry = Record.of("name", name);
					if (Files.isRegularFile(path)) {
						long size = Files.size(path);
						entry = entry.add("size", size);
					}
					out.send(entry);
				}
			} catch (IOException e) {
				err.send(Record.of("message", e.getMessage()));
			}
		}
	}

	public static class CurrentWorkDirectory implements Command, StateAware {

		private State state;

		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			out.send(Record.of("cwd", state.getCwd().toString()));
		}
	}

	public static class ChangeDirectory implements Command, StateAware {

		private State state;

		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args, Channel out, Channel err) {
			if (args.size() < 1) {
				err.send(Record.of("message", "missing path argument"));
				return;
			}
			Path newCwd = Paths.get(state.getCwd().toString(), args.get(0));
			if (Files.isDirectory(newCwd)) {
				state.setCwd(newCwd);
				out.send(Record.of("message", "now in " + state.getCwd().toString()));
			} else {
				err.send(Record.of("message", "not a directory"));
				return;
			}
		}

	}
}
