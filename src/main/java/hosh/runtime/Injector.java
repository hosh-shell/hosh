package hosh.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Resolves and inject services into -Aware classes. */
public class Injector {

	private final Map<Class<?>, Object> registry = new HashMap<>();

	public void register(Object object) {
		Objects.requireNonNull(object, "cannot register null object");
	}

}
