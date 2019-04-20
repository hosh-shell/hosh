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
package org.hosh.fitness;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hosh.BootstrapBuiltins;
import org.hosh.doc.Examples;
import org.hosh.doc.Help;
import org.hosh.runtime.CommandResolvers;
import org.hosh.runtime.Compiler;
import org.hosh.runtime.Compiler.CompileError;
import org.hosh.runtime.Compiler.Program;
import org.hosh.runtime.Parser.ParseError;
import org.hosh.spi.Command;
import org.hosh.spi.State;
import org.junit.jupiter.api.Test;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

public class DocFitnessTest {

	@Test
	public void checkExamplesSyntax() {
		State state = new State();
		BootstrapBuiltins bootstrapBuiltins = new BootstrapBuiltins();
		bootstrapBuiltins.registerAllBuiltins(state);
		assertThat(state.getCommands()).isNotEmpty();
		Compiler compiler = new Compiler(
				new CommandResolvers.AggregateCommandResolver(
						Arrays.asList(
								new CommandResolvers.BuiltinCommandResolver(state),
								new CommandResolvers.InfallibleResolver())));
		try (ScanResult scanResult = new ClassGraph().whitelistPackages("org.hosh").scan()) {
			scanResult
					.getClassesImplementing(Command.class.getCanonicalName())
					.loadClasses()
					.stream()
					.filter(c -> c.isAnnotationPresent(Examples.class))
					.map(c -> c.getAnnotation(Examples.class))
					.flatMap(examples -> Stream.of(examples.value()))
					.forEach(example -> {
						try {
							Program program = compiler.compile(example.command());
							assertThat(program).isNotNull();
						} catch (CompileError e) {
							throw new AssertionError("cannot compile " + example.command(), e);
						} catch (ParseError e) {
							throw new AssertionError("cannot parse " + example.command(), e);
						}
					});
		}
	}

	@Test
	public void displayHelpAndExamplesCoverage() {
		ClassGraph allClasses = new ClassGraph().whitelistPackages("org.hosh");
		try (ScanResult scanResult = allClasses.scan()) {
			List<Class<?>> commands = scanResult
					.getAllClasses()
					.loadClasses()
					.stream()
					.filter(c -> c.getEnclosingClass() != null && c.getEnclosingClass().getSimpleName().endsWith("Module"))
					.collect(Collectors.toList());
			List<Class<?>> withHelp = commands.stream().filter(c -> c.isAnnotationPresent(Help.class)).collect(Collectors.toList());
			List<Class<?>> withExamples = commands.stream().filter(c -> c.isAnnotationPresent(Examples.class))
					.collect(Collectors.toList());
			assertThat(withHelp).as("@Help").containsExactlyInAnyOrderElementsOf(commands);
			assertThat(withExamples).as("@Examples").containsExactlyInAnyOrderElementsOf(commands);
		}
	}
}
