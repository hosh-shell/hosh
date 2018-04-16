package org.hosh;

public interface Module {

    void beforeStart(CommandRegistry commandRegistry);

    void afterExit(CommandRegistry commandRegistry);

}
