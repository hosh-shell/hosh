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
					out.send(Record.empty().add("name", path.getFileName()).add("size", Files.size(path)));
				}
			} catch (IOException e) {
				err.send(Record.empty().add("message", e.getMessage()));
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
			out.send(Record.empty().add("cwd", state.getCwd().toString()));
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
				err.send(Record.empty().add("message", "missing path argument"));
				return;
			}
			Path newCwd = Paths.get(state.getCwd().toString(), args.get(0));
			if (Files.isDirectory(newCwd)) {
				state.setCwd(newCwd);
				out.send(Record.empty().add("message", "now in " + state.getCwd().toString()));
			} else {
				err.send(Record.empty().add("message", "not a directory"));
				return;
			}
		}

	}
}
