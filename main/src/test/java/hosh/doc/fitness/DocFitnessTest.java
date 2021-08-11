/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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
package hosh.doc.fitness;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import hosh.doc.Description;
import hosh.doc.Example;
import hosh.doc.Examples;
import hosh.runtime.BootstrapBuiltins;
import hosh.Hosh;
import hosh.runtime.CommandResolvers;
import hosh.runtime.Compiler;
import hosh.runtime.Compiler.CompileError;
import hosh.runtime.Compiler.Program;
import hosh.runtime.Parser.ParseError;
import hosh.spi.Command;
import hosh.runtime.MutableState;

import java.util.Map;
import java.util.function.Supplier;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fitness function to check that:
 *  - all commands are documented
 *  - all examples are syntactically correct
 *    (by compile execute all examples found as annotations)
 */
@AnalyzeClasses(packagesOf = Hosh.class)
public class DocFitnessTest {

	@SuppressWarnings("unused")
	@ArchTest
	public final ArchRule commandsAreDocumented =
		classes()
			.that().implement(Command.class)
			.and().arePublic()
			.should().beAnnotatedWith(Description.class)
			.andShould().beAnnotatedWith(Examples.class);

	@SuppressWarnings("unused")
	@ArchTest
	public final ArchRule commandsHaveSyntacticallyCorrectExamples =
		classes()
			.that().areAnnotatedWith(Examples.class)
			.and().arePublic()
			.should(beSyntacticallyCorrect());

	// implementation details
	private static ArchCondition<JavaClass> beSyntacticallyCorrect() {
		return new BeSyntacticallyCorrect();
	}

	private static class BeSyntacticallyCorrect extends ArchCondition<JavaClass> {

		private final Compiler compiler;

		private BeSyntacticallyCorrect() {
			super("be syntactically correct");
			BootstrapBuiltins bootstrapBuiltins = new BootstrapBuiltins();
			Map<String, Supplier<Command>> commands = bootstrapBuiltins.registerAllBuiltins();
			assertThat(commands).isNotEmpty();
			MutableState state = new MutableState();
			state.mutateCommands(commands);
			compiler = new Compiler(new CommandResolvers.BuiltinCommandResolver(state));
		}

		@Override
		public void check(JavaClass item, ConditionEvents events) {
			for (Example example : item.getAnnotationOfType(Examples.class).value()) {
				try {
					Program program = compiler.compile(example.command());
					assertThat(program).isNotNull();
					events.add(SimpleConditionEvent.satisfied(item, "working example"));
				} catch (CompileError e) {
					events.add(SimpleConditionEvent.violated(item, item + ": compile failed: " + e.getMessage()));
				} catch (ParseError e) {
					events.add(SimpleConditionEvent.violated(item, item + ": parse failed:" + e.getMessage()));
				}
			}
		}
	}

}
