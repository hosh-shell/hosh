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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Heavily inspired by JUnit 4.12 TemporaryFolder but with fewer features.
 * Notes:
 * - A more appropriate name could be "FileSystemTestHelper" or something like that;
 * - expose more methods to control file permissions, in a portable way;
 * - don't expose File in the public interface, whereas it is still ok to use File internally here when it makes sense.
 */
public class TemporaryFolder implements Extension, BeforeEachCallback, AfterEachCallback {

	private Path folder;

	public Path toPath() {
		return folder;
	}

	// Sonar is reporting: "Make sure publicly writable directories are used safely here."
	// since this call is used only for tests, it is fine.
	@SuppressWarnings("squid:S5443")
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		folder = Files.createTempDirectory("hosh-");
	}

	@Override
	public void afterEach(ExtensionContext context) {
		if (folder == null) {
			throw new IllegalStateException("folder is still null?");
		}
		recursiveDelete(folder.toFile());
	}

	public Path newFile(Path parent, String fileName) throws IOException {
		return Files.createFile(parent.resolve(fileName));
	}

	public Path newExecutableFile(String fileName) throws IOException {
		Path path = newFile(folder, fileName);
		if (!path.toFile().setExecutable(true)) {
			throw new IllegalStateException("failed to set executable bit for " + path);
		}
		return path;
	}

	public Path newFile(String fileName) throws IOException {
		return newFile(folder, fileName);
	}

	public Path newFolder(String folderName) throws IOException {
		return newFolder(folder, folderName);
	}

	public Path newFolder(Path parent, String folder) throws IOException {
		Path newFolder = parent.resolve(folder);
		return  Files.createDirectory(newFolder);
	}

	private void recursiveDelete(File fileOrDirectory) {
		File[] files = fileOrDirectory.listFiles();
		if (files != null) {
			for (File file : files) {
				recursiveDelete(file);
			}
		}
		boolean deleted = fileOrDirectory.delete();
		if (!deleted) {
			throw new IllegalStateException("file not deleted: " + fileOrDirectory);
		}
	}
}
