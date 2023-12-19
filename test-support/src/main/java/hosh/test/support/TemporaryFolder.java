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
import java.nio.file.Path;

/**
 * Heavily inspired by JUnit 4.12 TemporaryFolder but with fewer features.
 */
// we should not expose java.io.File here, tests usually want java.nio.file.Path
public class TemporaryFolder implements Extension, BeforeEachCallback, AfterEachCallback {

	private File folder;

	public File toFile() {
		return folder;
	}

	public Path toPath() {
		return folder.toPath();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		folder = createTemporary(null);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		if (folder == null) {
			throw new IllegalStateException("folder is still null?");
		}
		recursiveDelete(folder);
	}

	public File newFile(File parent, String fileName) throws IOException {
		File file = new File(parent, fileName);
		if (!file.createNewFile()) {
			throw new IOException("cannot create new file: " + file);
		}
		return file;
	}

	public File newFile(String fileName) throws IOException {
		return newFile(folder, fileName);
	}

	public File newFile() throws IOException {
		return File.createTempFile("hosh", null, folder);
	}

	public File newFolder(String folderName) throws IOException {
		return newFolder(folder, folderName);
	}

	public File newFolder(File parent, String folder) throws IOException {
		File newFolder = new File(parent, folder);
		if (!newFolder.mkdir()) {
			throw new IOException("cannot create new folder: " + newFolder);
		}
		return newFolder;
	}

	public File newFolder() throws IOException {
		return createTemporary(folder);
	}

	private void recursiveDelete(File fileOrDirectory) {
		File[] files = fileOrDirectory.listFiles();
		if (files != null) {
			for (File file : files) {
				recursiveDelete(file);
			}
		}
		fileOrDirectory.delete();
	}

	private File createTemporary(File parentFolder) throws IOException {
		File createdFolder = File.createTempFile("hosh", "", parentFolder);
		createdFolder.delete();
		createdFolder.mkdir();
		return createdFolder;
	}
}
