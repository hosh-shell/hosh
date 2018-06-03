package org.hosh.spi;

import javax.annotation.Nonnull;

/**
 * Handling of output of records
 * 
 * TOOD: by now it is only about interactive shell, it could be extended to
 * handle an out-of-band record with column names or printing JSON to a file.
 */
public interface Channel {

	void send(@Nonnull Record record);

}
