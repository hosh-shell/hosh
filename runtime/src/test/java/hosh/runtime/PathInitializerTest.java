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
package hosh.runtime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PathInitializerTest {

	private PathInitializer sut;

	@BeforeEach
	public void setUp() {
		sut = new PathInitializer();
	}

	@Test
	public void nullPathVariable() {
		List<Path> path = sut.initializePath(null);
		assertThat(path).isEmpty();
	}

	@Test
	public void emptyVariable() {
		List<Path> path = sut.initializePath("");
		assertThat(path).isEmpty();
	}

	@Test
	public void pathVariableWithOneElement() {
		List<Path> path = sut.initializePath("/bin");
		assertThat(path).containsExactly(Path.of("/bin"));
	}

	@Test
	public void pathVariableWithTwoElements() {
		List<Path> path = sut.initializePath("/bin" + File.pathSeparator + "/sbin");
		assertThat(path).containsExactly(Path.of("/bin"), Path.of("/sbin"));
	}

	@Test
	public void pathVariableWithEmptyElement() {
		List<Path> path = sut.initializePath("/bin" + File.pathSeparator + "   " + File.pathSeparator + "/sbin");
		assertThat(path).containsExactly(Path.of("/bin"), Path.of("/sbin"));
	}

	@EnabledOnOs(OS.WINDOWS)
	@Test
	public void pathVariableWithTrailingSpace() {
		List<Path> path = sut.initializePath("c:/Program Files "); // trailing space
		assertThat(path).containsExactly(Path.of("c:/Program Files"));
	}
}
