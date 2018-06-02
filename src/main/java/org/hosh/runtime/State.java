package org.hosh.runtime;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;

/**
 * Internal state of the shell:
 * <ul>
 *   <li>can be read freely</li>
 *   <li>can be modified only via controlled side-effecting actions</li>
 * </ul>
 * 
 * The state can be passed freely to components:
 * <ul> 
 *   <li>modules (e.g. help module)</li>
 *   <li>can use to produce some documentation or to change current working directory</li>
 * 	 <li>completer can use it to help the user to auto-complete commands</li>
 * 	 <li>distribute read-only configuration to every component</li>
 * </ul>
 */
public class State {

	// version of hosh, cannot be changed
	public static final String VERSION = "hosh.version";
	// current working directory, can be varied independently from Java property
	// user.dir
	public static final String CWD = "hosh.cwd";
	// commands registered
	public static final String COMMANDS = "hosh.commands";

	private final ConcurrentMap<String, Object> state = new ConcurrentHashMap<>();

	@Override
	public String toString() {
		return String.format("State[data=%s]", state);
	}

	@FunctionalInterface
	public interface Reader<T> {

		T get();

	}

	@FunctionalInterface
	public interface Writer<T> {

		void set(T newValue);

	}

	@SuppressWarnings("unchecked")
	public <T> Reader<T> registerReader(@Nonnull String key, @Nonnull T value) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		state.put(key, value); // TODO: check for duplicates?
		return () -> (T) state.get(key);
	}
	
	public <T> Writer<T> registerWriter(@Nonnull String key) {
		Objects.requireNonNull(key);
		return (newValue) -> state.put(key, newValue);
	}

}
