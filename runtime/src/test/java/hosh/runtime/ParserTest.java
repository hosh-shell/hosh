/*
 * MIT License
 *
 * Copyright (c) 2018-2021 Davide Angelocola
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
package hosh.runtime;

import hosh.runtime.antlr4.HoshParser.ProgramContext;
import hosh.doc.Bug;
import hosh.runtime.Parser.ParseError;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserTest {

	Parser sut;

	@BeforeEach
	void setup() {
		sut = new Parser();
	}

	@Test
	void lexerError() {
		assertThatThrownBy(() -> sut.parse("!")).isInstanceOf(ParseError.class)
			.hasMessage("line 1:0: token recognition error at: '!'");
	}

	@Test
	void refuseSpaceInsideVariableExpansion() {
		assertThatThrownBy(() -> sut.parse("${ EXECUTABLE }")).isInstanceOf(ParseError.class)
			.hasMessage("line 1:0: token recognition error at: '${ '");
	}

	@ParameterizedTest
	@MethodSource("all")
	void valid(String line) {
		ProgramContext parse = sut.parse(line);
		Assertions.assertThat(parse)
			.as("valid")
			.isNotNull();
	}

	static Stream<String> all() {
		return Stream.of(commands(), newLines(), comments(), pipelines(), incompletePipelines())
			       .flatMap(List::stream);
	}

	static List<String> commands() {
		return List.of(
			"",
			"/usr/bin/git; ls",
			"/usr/bin/git; ls;",
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
			"vim 'filename with newline\n'",
			"vim \"filename with spaces\"",
			"git commit -am \"commit message\"",
			"withTime { git push }",
			"withTime { git push | sink }",
			"withTime { git push | sink; }",
			"withTime { git push ; clear }",
			"withTime { git push ; clear; }",
			"withLock /tmp/push.lock { git push }",
			"withLock /tmp/push.lock { git push; git push --tags }",
			"withTime { withLock /tmp/push.lock { git push } }",
			"cd C:\\Users\\VSSADM~1\\AppData\\Local\\Temp\\junit16864313966026428034",
			"regex line '\\w+'",
			"regex line \"\\w+\"",
			"ls ${VAR!/tmp}",
			"echo ${HELLO}${WHO}",
			"ls ${JAVA_HOME}/bin",
			"ls ${JAVA_HOME}/${JVM_BINARY}",
			"ls \"${JAVA_HOME}\"",
			"ls \"${JAVA_HOME}/${JVM_BINARY}\"",
			"echo 'òàù\"è+ì|!£$%&/()=?^'",
			"echo \"òàù'è+ì|!£&/()=?^\"",
			"echo '{}|'",
			"echo \"{}|\"",
			"echo ''", // empty string
			"echo '       '" // spaces
		);
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
			"env | sink",
			"git config --list | sink",
			"git config --list | grep -v author",
			"git config --list |\ngrep -v author",
			"/usr/bin/git diff | grep /regexp/",
			"env | grep /regexp/",
			"cat file.txt | grep /regexp/",
			"cat file.txt | grep /regexp/ | wc -l",
			"withTime { cat file.txt | grep /regexp/ | wc -l }",
			"withTime { cat file.txt } | schema",
			"withTime { cat file.txt | grep /regexp/ } | schema",
			"walk . | glob '*'",
			"walk . | glob '*.{jar,war}'", // regression test for #205
			"walk . | glob '*.[ch]'"
		);
	}

	@Bug(issue = "https://github.com/dfa1/hosh/issues/26", description = "accepted by parser but rejected by the compiler")
	static List<String> incompletePipelines() {
		return List.of(
			"ls | ",
			"ls -a | ",
			"ls | take 1 | ",
			"ls | take | ");
	}
}
