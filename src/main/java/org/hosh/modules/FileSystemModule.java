package org.hosh.modules;

import org.hosh.Command;
import org.hosh.CommandRegistry;
import org.hosh.Module;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileSystemModule implements Module {

    @Override
    public void beforeStart(CommandRegistry commandRegistry) {
        commandRegistry.registerCommand("ls", ListFiles.class);
        commandRegistry.registerCommand("cwd", CurrentWorkDirectory.class);
    }

    @Override
    public void afterExit(CommandRegistry commandRegistry) {

    }

    public static class ListFiles implements Command {

        @Override
        public void run(List<String> args) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."))) {
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
            System.out.println(Paths.get(".").toAbsolutePath().normalize().toString());
        }
    }

}
