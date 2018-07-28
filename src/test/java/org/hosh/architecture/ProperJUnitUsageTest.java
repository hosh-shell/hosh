package org.hosh.architecture;

import static org.junit.Assert.fail;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

public class ProperJUnitUsageTest {
	@Test
	public void enforcePresenceOfTestAnnotation() {
		new FastClasspathScanner("-jar:")
				.scan()
				.getNamesOfAllClasses()
				.stream()
				.filter(n -> n.startsWith("org.hosh"))
				.filter(n -> n.endsWith("Test") || n.endsWith("IT"))
				.map(this::load)
				.flatMap(c -> Stream.of(c.getDeclaredMethods()))
				.filter(m -> Modifier.isPublic(m.getModifiers()))
				.filter(m -> !Modifier.isStatic(m.getModifiers()))
				.filter(m -> !m.isAnnotationPresent(Before.class))
				.filter(m -> !m.isAnnotationPresent(After.class))
				.filter(m -> !m.isAnnotationPresent(Test.class))
				.forEach(m -> {
					fail("method should be annotated with @Test: " + m);
				});
	}

	private Class<?> load(String name) {
		try {
			return Class.forName(name);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
