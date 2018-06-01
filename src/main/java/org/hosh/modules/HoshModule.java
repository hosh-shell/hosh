package org.hosh.modules;

import org.hosh.spi.Command;
import org.hosh.spi.CommandRegistry;
import org.hosh.spi.Module;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HoshModule implements Module {

    @Override
    public void onStartup(@Nonnull CommandRegistry commandRegistry) {
        commandRegistry.registerCommand("env", Env.class);
        commandRegistry.registerCommand("info", Info.class);
        commandRegistry.registerCommand("exit", Exit.class);
    }

    public static class Env implements Command {

        @Override
        public void run(List<String> args) {
            Map<String, String> env = System.getenv();
            for (Map.Entry<String, String> entry : env.entrySet()) {
                System.out.printf("%s = %s%n", entry.getKey(), entry.getValue());
            }
        }

    }

    public static class Info implements Command {

        @Override
        public void run(List<String> args) {
            System.out.printf("locale = %s%n", Locale.getDefault());
            System.out.printf("timezone = %s%n", TimeZone.getDefault().getID());
        }

    }

    public static class Exit implements Command {

        @Override
        public void run(List<String> args) {
            System.exit(0);
        }

    }

}
