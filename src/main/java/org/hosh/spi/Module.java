package org.hosh.spi;

import javax.annotation.Nonnull;

public interface Module {

    void onStartup(@Nonnull CommandRegistry commandRegistry);

}
