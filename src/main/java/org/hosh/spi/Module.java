package org.hosh.spi;

import org.hosh.runtime.CommandRegistry;

import javax.annotation.Nonnull;

public interface Module {

    void onStartup(@Nonnull CommandRegistry commandRegistry);

}
