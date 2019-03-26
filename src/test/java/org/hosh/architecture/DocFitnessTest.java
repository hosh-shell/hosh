/**
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
package org.hosh.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.hosh.BootstrapBuiltins;
import org.hosh.doc.Example;
import org.hosh.doc.Examples;
import org.hosh.doc.Help;
import org.hosh.doc.Todo;
import org.hosh.runtime.CommandResolvers;
import org.hosh.runtime.Compiler;
import org.hosh.runtime.Compiler.Program;
import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.junit.Test;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class DocFitnessTest {

	@Test
	public void checkExamplesSytanx() {
		State state = new State();
		BootstrapBuiltins bootstrapBuiltins = new BootstrapBuiltins();
		bootstrapBuiltins.registerAllBuiltins(state);
		assertThat(state.getCommands()).isNotEmpty();
		Compiler compiler = new Compiler(new CommandResolvers.BuiltinCommandResolver(state));
		try (ScanResult scanResult = new ClassGraph().whitelistPackages("org.hosh").scan()) {
			scanResult
					.getClassesImplementing(Command.class.getCanonicalName())
					.loadClasses()
					.stream()
					.filter(c -> c.isAnnotationPresent(Examples.class))
					.map(c -> c.getAnnotation(Examples.class))
					.flatMap(examples -> Stream.of(examples.value()))
					.forEach(example -> {
						Program program = compiler.compile(example.command());
						assertThat(program).isNotNull();
					});
		}
	}

	@Todo(description = "enforce this fitness test after 100% coverage is reached")
	@Test
	public void displayHelpAndExamplesCoverage() {
		try (ScanResult scanResult = new ClassGraph().whitelistPackages("org.hosh").scan()) {
			List<Class<?>> commands = scanResult
					.getClassesImplementing(Command.class.getCanonicalName())
					.loadClasses();
			long totalCommands = commands.size();
			long withHelp = commands.stream().filter(c -> c.isAnnotationPresent(Help.class)).count();
			long withExamples = commands.stream().filter(c -> c.isAnnotationPresent(Examples.class) || c.isAnnotationPresent(Example.class)).count();
			System.out.printf("@Examples %s/%s%n", withExamples, totalCommands);
			System.out.printf("@Help     %s/%s%n", withHelp, totalCommands);
		}
	}
}
