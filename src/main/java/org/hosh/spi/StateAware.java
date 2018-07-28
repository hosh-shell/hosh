package org.hosh.spi;

/** Marker interface to inject State in Commands */
public interface StateAware {
	void setState(State state);
}
