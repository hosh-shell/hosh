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
package hosh.test.fitness;

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;

/**
 * Fitness function to check:
 * - methods in tests are well encapsulated (e.g. no public utility methods, etc.)
 * - all @Mocks are used (@InjectMock prevents to see that easily)
 */
public abstract class UnitTestsFitnessTest {

	@SuppressWarnings("unused")
	@ArchTest
	public static final ArchRule UNANNOTATED_METHODS_IN_TESTS_MUST_BE_PRIVATE =
		ArchRuleDefinition.methods().that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Test")
			.or().areDeclaredInClassesThat().haveSimpleNameEndingWith("IT")
			.and().areNotPrivate()
			.and().areNotStatic()
			.should().beAnnotatedWith(Test.class)
			.orShould().beAnnotatedWith(BeforeEach.class)
			.orShould().beAnnotatedWith(AfterEach.class)
			.orShould().beAnnotatedWith(ParameterizedTest.class);

	@SuppressWarnings("unused")
	@ArchTest
	public static final ArchRule MOCKS_MUST_BE_USED =
		ArchRuleDefinition.fields()
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
				events.add(SimpleConditionEvent.violated(item, "unused @Mock " + item.getFullName()));
			}
		}
	}
}
