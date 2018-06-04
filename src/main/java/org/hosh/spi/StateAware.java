package org.hosh.spi;

import javax.annotation.Nonnull;

/** Marker interface to inject State in Commands */
public interface StateAware {

	void setState(@Nonnull State state);
	
}
