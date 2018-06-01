package org.hosh.modules;

import org.hosh.spi.Command;
import org.hosh.runtime.CommandRegistry;
import org.hosh.spi.Module;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileSystemModule implements Module {

    // XXX: probably this will be needed by several subsystems (i.e. prompt, process handling)
    private static Path currentWorkingDirectory = Paths.get(".");

    @Override
    public void onStartup(@Nonnull CommandRegistry commandRegistry) {
        commandRegistry.registerCommand("cd", ChangeDirectory.class);
        commandRegistry.registerCommand("ls", ListFiles.class);
        commandRegistry.registerCommand("cwd", CurrentWorkDirectory.class);
    }

    public static class ListFiles implements Command {

        @Override
        public void run(List<String> args) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(FileSystemModule.currentWorkingDirectory)) {
                for (Path path : stream) {
                    System.out.println(path.getFileName() + " " + Files.size(path));
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public static class CurrentWorkDirectory implements Command {

        @Override
        public void run(List<String> args) {
            System.out.println(FileSystemModule.currentWorkingDirectory.toAbsolutePath().normalize().toString());
        }
    }

    public static class ChangeDirectory implements Command {

        @Override
        public void run(List<String> args) {
            if (args.size() < 1) {
                System.err.println("missing path");
                return;
            }
            FileSystemModule.currentWorkingDirectory = Paths.get(args.get(0));
        }

    }
}
