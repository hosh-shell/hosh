/**
 * MIT License
 *
 * Copyright (c) 2017-2018 Davide Angelocola
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
package org.hosh.runtime;

import org.hosh.doc.Bug;
import org.hosh.runtime.Parser.ParseError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ParserTest {
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	private Parser sut = new Parser();

	@Test
	public void empty() {
		sut.parse("");
	}

	@Test
	public void usage() {
		sut.parse("git");
		sut.parse("git\n");
		sut.parse("git status\n");
		sut.parse("git commit --amend\n");
		sut.parse("cd ..\n");
		sut.parse("cd /tmp\n");
		sut.parse("cd c:\\temp\n");
		sut.parse("cd c:/temp\n");
		sut.parse("${EXECUTABLE}");
		sut.parse("cd ${DIR}");
		sut.parse("vim 'filename with spaces'");
		sut.parse("vim \"filename with spaces\"");
		sut.parse("git commit -am \"commit message\"");
		sut.parse("withTime { git push }");
		sut.parse("withLock /tmp/push.lock { git push }");
		sut.parse("withLock /tmp/push.lock { git push\n git push --tags\n }");
		sut.parse("withTime { withLock /tmp/push.lock { git push } }");
	}

	@Test
	public void newlines() {
		sut.parse("\n");
		sut.parse("\n\n");
		sut.parse("\r\n");
		sut.parse("\r\n\n");
		sut.parse("\n\r\n");
	}

	@Test
	public void comments() {
		sut.parse("#\n");
		sut.parse("#\r\n");
		sut.parse("# comment\n");
		sut.parse("# comment\r\n");
		sut.parse("ls # comment\r\n");
	}

	@Test
	public void pipelines() {
		sut.parse("env | grep /regexp/");
		sut.parse("cat file.txt | grep /regexp/");
		sut.parse("cat file.txt | grep /regexp/ | wc -l");
		sut.parse("withTime { cat file.txt | grep /regexp/ | wc -l }");
	}

	@Test
	public void lexerError() {
		expectedException.expect(ParseError.class);
		expectedException.expectMessage("line 1:0: token recognition error at: '!'");
		sut.parse("!");
	}

	@Test
	public void refuseSpaceInsideVariableExpansion() {
		expectedException.expect(ParseError.class);
		expectedException.expectMessage("line 1:0: token recognition error at: '${ '");
		sut.parse("${ EXECUTABLE }");
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/26", description = "rejected by the compiler")
	@Test
	public void incompletePipeline() {
		sut.parse("ls | take 1 | ");
		sut.parse("ls | take | ");
	}
}