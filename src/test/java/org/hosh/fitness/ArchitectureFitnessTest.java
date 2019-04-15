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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Enforcing package dependencies of the project:
 * <ul>
 * <li>modules can see only spi</li>
 * <li>runtime can see spi but spi cannot see runtime</li>
 * </ul>
 * The long term goal is to prepare ground for modules.
 */
public class ArchitectureFitnessTest {

	@Test
	public void enforceProperDependenciesBetweenPackages() {
		JavaClasses importedClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
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
