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
package org.hosh.script;

import hosh.doc.Experimental;
import hosh.doc.Todo;

import java.io.IOException;
import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

@Todo(description = "ignored completely Bindings and scope requirement")
@Experimental(description = "preliminary support for JSR-223", issue = "https://github.com/dfa1/hosh/issues/105")
public class HoshScriptEngine implements ScriptEngine {

	private final HoshScriptEngineFactory scriptEngineFactory;

	private ScriptContext scriptContext;

	public HoshScriptEngine(HoshScriptEngineFactory scriptEngineFactory) throws IOException {
		this.scriptEngineFactory = scriptEngineFactory;
		this.scriptContext = new HoshScriptContext();
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return scriptEngineFactory;
	}

	@Override
	public Object eval(String script, ScriptContext context) throws ScriptException {
		if (!(context instanceof HoshScriptContext)) {
			throw new IllegalArgumentException();
		}
		HoshScriptContext hoshScriptContext = (HoshScriptContext) context;
		try {
			return hoshScriptContext.eval(script);
		} catch (Exception e) {
			throw new ScriptException(e);
		}
	}

	@Override
	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
		try {
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = reader.read()) != -1) {
				sb.append((char) c);
			}
			return eval(sb.toString(), context);
		} catch (Exception e) {
			throw new ScriptException(e);
		}
	}

	@Override
	public Object eval(String script) throws ScriptException {
		return eval(script, scriptContext);
	}

	@Override
	public Object eval(Reader reader) throws ScriptException {
		return eval(reader, scriptContext);
	}

	@Override
	public Object eval(String script, Bindings n) throws ScriptException {
		return eval(script, scriptContext);
	}

	@Override
	public Object eval(Reader reader, Bindings n) throws ScriptException {
		return eval(reader, scriptContext);
	}

	@Override
	public void put(String key, Object value) {
	}

	@Override
	public Object get(String key) {
		return null;
	}

	@Override
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	@Override
	public Bindings getBindings(int scope) {
		return new SimpleBindings();
	}

	@Override
	public void setBindings(Bindings bindings, int scope) {
	}

	@Override
	public ScriptContext getContext() {
		return scriptContext;
	}

	@Override
	public void setContext(ScriptContext context) {
		this.scriptContext = context;
	}
}
