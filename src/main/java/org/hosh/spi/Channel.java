package org.hosh.spi;

/**
 * Handling of output of records.
 */
public interface Channel {

	void send(Record record);

}
