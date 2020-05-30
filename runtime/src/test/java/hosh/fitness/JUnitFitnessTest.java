/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Davide Angelocola
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

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import hosh.runtime.Hosh;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Enforcing some useful rules for junit tests.
 *
 * @deprecated adapt to modular build or remove it
 */
@AnalyzeClasses(packagesOf = Hosh.class)
public class JUnitFitnessTest {

	@ArchTest
	public final ArchRule unannotatedMethodsInTests =
		methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Test")
			.or().areDeclaredInClassesThat().haveSimpleNameEndingWith("IT")
			.and().areNotPrivate()
			.should().beAnnotatedWith(Test.class)
			.orShould().beAnnotatedWith(BeforeEach.class)
			.orShould().beAnnotatedWith(AfterEach.class)
			.orShould().beAnnotatedWith(ParameterizedTest.class);

	@ArchTest
	public final ArchRule unusedMocks =
		fields()
			.that()
			.areNotStatic().and().areAnnotatedWith(Mock.class)
			.should(haveAccesses());

	// implementation details
	private static HaveAccesses haveAccesses() {
		return new HaveAccesses();
	}

	private static class HaveAccesses extends ArchCondition<JavaField> {

		public HaveAccesses() {
			super("be used used or removed");
		}

		@Override
		public void check(JavaField item, ConditionEvents events) {
			if (item.getAccessesToSelf().isEmpty()) {
				events.add(SimpleConditionEvent.violated(item,"unused @Mock " + item.getFullName()));
			}
		}
	}
}
