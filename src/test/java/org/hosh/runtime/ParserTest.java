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
package org.hosh.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;

import org.hosh.antlr4.HoshParser.ProgramContext;
import org.hosh.doc.Bug;
import org.hosh.doc.Todo;
import org.hosh.runtime.Parser.ParseError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ParserTest {

	private Parser sut;

	@BeforeEach
	public void setup() {
		sut = new Parser();
	}

	@Test
	public void lexerError() {
		assertThatThrownBy(() -> {
			sut.parse("!");
		}).isInstanceOf(ParseError.class)
				.hasMessage("line 1:0: token recognition error at: '!'");
	}

	@Test
	public void refuseSpaceInsideVariableExpansion() {
		assertThatThrownBy(() -> {
			sut.parse("${ EXECUTABLE }");
		}).isInstanceOf(ParseError.class)
				.hasMessage("line 1:0: token recognition error at: '${ '");
	}

	@ParameterizedTest
	@MethodSource("all")
	public void valid(String line) {
		ProgramContext parse = sut.parse(line);
		assertThat(parse)
				.as("valid")
				.isNotNull();
	}

	@Todo(description = "workaroud for eclipse bug", issue = "https://bugs.eclipse.org/bugs/show_bug.cgi?id=546084")
	static Stream<String> all() {
		return List.of(commands(), newLines(), comments(), pipelines(), incompletePipelines())
				.stream()
				.flatMap(List::stream);
	}

	static List<String> commands() {
		return List.of(
				"/usr/bin/git",
				"/usr/bin/git --help",
				"   /usr/bin/git",
				"/usr/bin/git   ",
				"   /usr/bin/git    ",
				"\t/usr/bin/git\t",
				"\t/usr/bin/git",
				"git",
				"git\n",
				"git status\n",
				"git commit --amend\n",
				"cd ..\n",
				"cd /tmp\n",
				"cd c:\\temp\n",
				"cd c:/temp\n",
				"cd ${DIR}",
				"vim 'filename with spaces'",
				"vim \"filename with spaces\"",
				"git commit -am \"commit message\"",
				"withTime { git push }",
				"withLock /tmp/push.lock { git push }",
				"withLock /tmp/push.lock { git push\n git push --tags\n }",
				"withTime { withLock /tmp/push.lock { git push } }",
				"cd C:\\Users\\VSSADM~1\\AppData\\Local\\Temp\\junit16864313966026428034",
				"regex line '\\w+'",
				"regex line \"\\w+\"",
				"ls ${VAR!/tmp}");
	}

	static List<String> newLines() {
		return List.of(
				"\n",
				"\n\n",
				"\r\n",
				"\r\n\n",
				"\n\r\n");
	}

	static List<String> comments() {
		return List.of(
				"#\n",
				"#\r\n",
				"# comment\n",
				"# comment\r\n",
				"ls # comment\n",
				"ls # comment\r\n",
				"ls # comment",
				"benchmark { ls | sink } # comment");
	}

	static List<String> pipelines() {
		return List.of(
				"/usr/bin/git diff | grep /regexp/",
				"env | grep /regexp/",
				"cat file.txt | grep /regexp/",
				"cat file.txt | grep /regexp/ | wc -l",
				"withTime { cat file.txt | grep /regexp/ | wc -l }",
				"withTime { cat file.txt } | schema",
				"withTime { cat file.txt | grep /regexp/ } | schema");
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/26", description = "rejected by the compiler")
	static List<String> incompletePipelines() {
		return List.of(
				"ls | ",
				"ls -a | ",
				"ls | take 1 | ",
				"ls | take | ");
	}
}