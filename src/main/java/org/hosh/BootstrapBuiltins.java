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
package org.hosh;

import java.util.ServiceLoader;

import org.hosh.doc.BuiltIn;
import org.hosh.runtime.CommandRegistry;
import org.hosh.runtime.SimpleCommandRegistry;
import org.hosh.spi.Command;
import org.hosh.spi.Module;
import org.hosh.spi.State;

/**
 * Registers all built-in command, used in both production and test.
 */
public class BootstrapBuiltins {

	@SuppressWarnings("unchecked")
	public void registerAllBuiltins(State state) {
		CommandRegistry commandRegistry = new SimpleCommandRegistry(state);
		ServiceLoader<Module> modules = ServiceLoader.load(Module.class);
		for (Module module : modules) {
			Class<?>[] declaredClasses = module.getClass().getDeclaredClasses();
			for (Class<?> declaredClass : declaredClasses) {
				if (Command.class.isAssignableFrom(declaredClass)) {
					BuiltIn builtIn = declaredClass.getAnnotation(BuiltIn.class);
					commandRegistry.registerCommand(builtIn.name(), (Class<? extends Command>) declaredClass);
				}
			}
		}
	}
}
