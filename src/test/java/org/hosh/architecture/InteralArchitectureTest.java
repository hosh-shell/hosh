package org.hosh.architecture;


import org.junit.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Enforcing general dependencies of the project:
 * 
 * - modules can see only spi
 * - runtime can see spi but spi cannot see runtime
 */
public class InteralArchitectureTest {

	@Test
	public void properModulesDependencies() {
		JavaClasses importedClasses = new ClassFileImporter().importPackages("org.hosh");

		classes()
			.that().resideInAPackage("..modules..")
			.should().accessClassesThat().resideInAnyPackage("..spi..", "java..")
			.check(importedClasses);
		
		noClasses()
			.that().resideInAPackage("..modules..")
			.should().accessClassesThat().resideInAPackage("..runtime..")
			.check(importedClasses);
		
		noClasses()
			.that().resideInAPackage("..spi..")
			.should().accessClassesThat().resideInAPackage("..runtime..")
			.check(importedClasses);
	}

}
