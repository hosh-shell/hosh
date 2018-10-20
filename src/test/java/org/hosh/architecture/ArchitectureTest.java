package org.hosh.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Enforcing general dependencies of the project:
 * <ul>
 * <li>modules can see only spi</li>
 * <li>runtime can see spi but spi cannot see runtime</li>
 * </ul>
 */
public class ArchitectureTest {
	@Test
	public void enforceProperDependenciesBetweenPackages() {
		JavaClasses importedClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DONT_INCLUDE_JARS)
				.withImportOption(ImportOption.Predefined.DONT_INCLUDE_ARCHIVES)
				.importPackages("org.hosh");
		slices().matching("org.hosh").should().beFreeOfCycles();
		classes().that().resideInAPackage("..modules..")
				.should().accessClassesThat().resideInAnyPackage("..spi..", "java..")
				.check(importedClasses);
		noClasses().that().resideInAPackage("..modules..")
				.should().accessClassesThat().resideInAPackage("..runtime..")
				.check(importedClasses);
		noClasses().that().resideInAPackage("..spi..")
				.should().accessClassesThat().resideInAPackage("..runtime..")
				.check(importedClasses);
	}
}
