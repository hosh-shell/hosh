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
package org.hosh.testsupport;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Heavily inspired by JUnit 4.12 TemporaryFolder
 */
public class TemporaryFolder implements Extension, BeforeEachCallback, AfterEachCallback {

	private File folder;

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		folder = createTemporaryFolderIn(null);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		if (folder != null) {
			recursiveDelete(folder);
		}
	}

	private void recursiveDelete(File file) {
		File[] files = file.listFiles();
		if (files != null) {
			for (File each : files) {
				recursiveDelete(each);
			}
		}
		file.delete();
	}

	/**
	 * Returns a new fresh file with the given name under the temporary folder.
	 */
	public File newFile(String fileName) throws IOException {
		File file = new File(getRoot(), fileName);
		if (!file.createNewFile()) {
			throw new IOException(
					"a file with the name \'" + fileName + "\' already exists in the test folder");
		}
		return file;
	}

	/**
	 * Returns a new fresh file with a random name under the temporary folder.
	 */
	public File newFile() throws IOException {
		return File.createTempFile("junit", null, getRoot());
	}

	/**
	 * Returns a new fresh folder with the given name under the temporary
	 * folder.
	 */
	public File newFolder(String folder) throws IOException {
		return newFolder(new String[] { folder });
	}

	/**
	 * Returns a new fresh folder with the given name(s) under the temporary
	 * folder.
	 */
	public File newFolder(String... folderNames) throws IOException {
		File file = getRoot();
		for (int i = 0; i < folderNames.length; i++) {
			String folderName = folderNames[i];
			validateFolderName(folderName);
			file = new File(file, folderName);
			if (!file.mkdir() && isLastElementInArray(i, folderNames)) {
				throw new IOException(
						"a folder with the name \'" + folderName + "\' already exists");
			}
		}
		return file;
	}

	/**
	 * Validates if multiple path components were used while creating a folder.
	 * 
	 * @param folderName
	 *            Name of the folder being created
	 */
	private void validateFolderName(String folderName) throws IOException {
		File tempFile = new File(folderName);
		if (tempFile.getParent() != null) {
			String errorMsg = "Folder name cannot consist of multiple path components separated by a file separator."
					+ " Please use newFolder('MyParentFolder','MyFolder') to create hierarchies of folders";
			throw new IOException(errorMsg);
		}
	}

	private boolean isLastElementInArray(int index, String[] array) {
		return index == array.length - 1;
	}

	/**
	 * Returns a new fresh folder with a random name under the temporary folder.
	 */
	public File newFolder() throws IOException {
		return createTemporaryFolderIn(getRoot());
	}

	private File createTemporaryFolderIn(File parentFolder) throws IOException {
		File createdFolder = File.createTempFile("junit", "", parentFolder);
		createdFolder.delete();
		createdFolder.mkdir();
		return createdFolder;
	}

	/**
	 * @return the location of this temporary folder.
	 */
	public File getRoot() {
		return folder;
	}
}
