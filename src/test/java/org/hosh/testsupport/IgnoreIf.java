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
package org.hosh.testsupport;

import static org.junit.Assume.assumeFalse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import org.hosh.doc.Experimental;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@Experimental(description = "JUnit 5 provides a much more complete solution to this problem")
public class IgnoreIf implements MethodRule {

	interface Condition {

		boolean test();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface IgnoredIf {

		String description();

		Class<? extends Condition> condition();
	}

	@Override
	public Statement apply(Statement base, FrameworkMethod method, Object target) {
		IgnoredIf annotation = method.getAnnotation(IgnoredIf.class);
		if (annotation == null) {
			return base;
		}
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				Condition condition = annotation.condition().getDeclaredConstructor().newInstance();
				assumeFalse("ignored by condition: " + annotation.condition(), condition.test());
				base.evaluate();
			}
		};
	}

	public static class OnWindows implements Condition {

		@Override
		public boolean test() {
			return normalizedOsName().contains("win");
		}
	}

	public static class NotOnWindows implements Condition {

		@Override
		public boolean test() {
			return !normalizedOsName().contains("win");
		}
	}

	private static String normalizedOsName() {
		return System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
	}
}
