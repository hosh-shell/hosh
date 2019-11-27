/*
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
package hosh.script;

import hosh.doc.Experimental;
import hosh.runtime.VersionLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

@Experimental(description = "preliminary support for JSR-223", issue = "https://github.com/dfa1/hosh/issues/105")
public class HoshScriptEngineFactory implements ScriptEngineFactory {

	private static final String NAME = "hosh";

	private static final String VERSION = version();

	private static String version() {
		try {
			return VersionLoader.loadVersion();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private final Map<String, String> properties = Map.ofEntries(
			Map.entry(ScriptEngine.ENGINE, NAME),
			Map.entry(ScriptEngine.ENGINE_VERSION, VERSION),
			Map.entry(ScriptEngine.NAME, NAME),
			Map.entry(ScriptEngine.LANGUAGE, NAME),
			Map.entry(ScriptEngine.LANGUAGE_VERSION, VERSION));

	@Override
	public Object getParameter(String key) {
		return properties.get(key);
	}

	@Override
	public String getEngineName() {
		return properties.get(ScriptEngine.ENGINE);
	}

	@Override
	public String getEngineVersion() {
		return properties.get(ScriptEngine.ENGINE_VERSION);
	}

	@Override
	public List<String> getExtensions() {
		return List.of("hosh");
	}

	@Override
	public List<String> getMimeTypes() {
		return List.of();
	}

	@Override
	public List<String> getNames() {
		return List.of("hosh");
	}

	@Override
	public String getLanguageName() {
		return properties.get(ScriptEngine.LANGUAGE);
	}

	@Override
	public String getLanguageVersion() {
		return properties.get(ScriptEngine.LANGUAGE_VERSION);
	}

	@Override
	public ScriptEngine getScriptEngine() {
		try {
			return new HoshScriptEngine(this);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// still unclear how to best implement these
	@Override
	public String getMethodCallSyntax(String obj, String m, String... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOutputStatement(String toDisplay) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getProgram(String... statements) {
		throw new UnsupportedOperationException();
	}
}
