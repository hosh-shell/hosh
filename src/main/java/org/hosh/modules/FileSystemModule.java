package org.hosh.modules;

import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;
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
		public void run(List<String> args) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(state.getCwd())) {
				for (Path path : stream) {
					System.out.println(path.getFileName() + " " + Files.size(path));
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public static class CurrentWorkDirectory implements Command, StateAware {

		private State state;

		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args) {
			System.out.println(state.getCwd().toString());
		}
	}

	public static class ChangeDirectory implements Command, StateAware {

		private State state;

		public void setState(State state) {
			this.state = state;
		}

		@Override
		public void run(List<String> args) {
			if (args.size() < 1) {
				System.err.println("missing path");
				return;
			}
			Path newCwd = Paths.get(state.getCwd().toString(), args.get(0));
			if (Files.isDirectory(newCwd)) {
				state.setCwd(newCwd);
			} else {
				System.err.println("not a directory");
				return;
			}
		}

	}
}
