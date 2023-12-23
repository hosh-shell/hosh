/*
 * MIT License
 *
 * Copyright (c) 2018-2023 Davide Angelocola
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
package hosh.test.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class TemporaryFolderTest {

	@RegisterExtension
	final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	void usage() throws IOException {
		// must always provide a good base directory
		assertThat(temporaryFolder.toPath())
				.isNotNull()
				.exists()
				.isDirectory()
		;

		// create a new directory
		assertThat(temporaryFolder.newFolder("folder"))
				.isNotNull()
				.exists()
				.isDirectory()
		;

		// create a new file
		assertThat(temporaryFolder.newFile("file.txt"))
				.isNotNull()
				.exists()
				.isEmptyFile()
		;

		// create a new file under a new directory
		assertThat(temporaryFolder.newFile(temporaryFolder.newFolder("sub-folder"), "file.txt"))
				.isNotNull()
				.exists()
				.isEmptyFile()
		;

		// cleanup
		temporaryFolder.afterEach(null);
		assertThat(temporaryFolder.toPath()).doesNotExist();
	}

}