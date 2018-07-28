package org.hosh.architecture;

import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

import org.junit.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

public class CodingRulesTest {
	private final JavaClasses classes = new ClassFileImporter().importPackages("org.hosh");

	@Test
	public void classes_should_not_throw_generic_exceptions() {
		NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(classes);
	}

	@Test
	public void classes_should_not_use_java_util_logging() {
		NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(classes);
	}
}
