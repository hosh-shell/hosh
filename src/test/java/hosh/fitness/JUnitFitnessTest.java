/*
 * MIT License
 *
 * Copyright (c) 2018-2019 Davide Angelocola
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package hosh.fitness;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import hosh.Hosh;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class JUnitFitnessTest {

	@Test
	public void enforcePresenceOfTestAnnotation() {

		try (ScanResult scanResult = new ClassGraph().whitelistPackages(Hosh.class.getPackageName()).scan()) {
			assertThat(scanResult.getAllClasses()).isNotEmpty();
			List<Method> methodsMissingTestAnnotation = scanResult
					.getAllClasses()
					.loadClasses()
					.stream()
					.filter(c -> c.getName().endsWith("Test") || c.getName().endsWith("IT"))
					.flatMap(c -> Stream.of(c.getDeclaredMethods()))
					.filter(m -> Modifier.isPublic(m.getModifiers()))
					.filter(m -> !Modifier.isStatic(m.getModifiers()))
					.filter(m -> m.getDeclaredAnnotation(Test.class) == null)
					.filter(m -> m.getDeclaredAnnotation(ParameterizedTest.class) == null)
					.filter(m -> m.getDeclaredAnnotation(BeforeEach.class) == null)
					.filter(m -> m.getDeclaredAnnotation(AfterEach.class) == null)
					.collect(Collectors.toList());
			assertThat(methodsMissingTestAnnotation)
					.overridingErrorMessage("please add @Test to the following methods: %n%s", methodsMissingTestAnnotation)
					.isEmpty();
		}
	}
}
