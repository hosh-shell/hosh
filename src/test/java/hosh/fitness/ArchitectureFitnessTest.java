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

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import hosh.Hosh;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Enforcing package dependencies of the project:
 * <ul>
 * <li>modules can see only spi</li>
 * <li>runtime can see spi but spi cannot see runtime</li>
 * </ul>
 * The long term goal is to prepare ground for modules.
 */
@AnalyzeClasses(packagesOf = Hosh.class)
public class ArchitectureFitnessTest {

	@ArchTest
	public final ArchRule noCycles =
		slices().matching(Hosh.class.getPackageName()).should().beFreeOfCycles();

	@ArchTest
	public final ArchRule modulesCanUseSpi =
		classes().that().resideInAPackage("..modules..")
			.should().accessClassesThat().resideInAnyPackage("..spi..", "java..");

	@ArchTest
	public final ArchRule modulesCannotUseRuntime =
		noClasses().that().resideInAPackage("..modules..")
			.should().accessClassesThat().resideInAPackage("..runtime..");

	@ArchTest
	public final ArchRule spiCannotUseRuntime =
		noClasses().that().resideInAPackage("..spi..")
			.should().accessClassesThat().resideInAPackage("..runtime..");

	@ArchTest
	public final ArchRule noReflection =
		noClasses()
			.should().accessClassesThat().resideInAPackage("java.lang.reflect");
}
