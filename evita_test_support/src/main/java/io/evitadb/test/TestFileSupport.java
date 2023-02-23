/*
 *                         _ _        ____  ____
 *               _____   _(_) |_ __ _|  _ \| __ )
 *              / _ \ \ / / | __/ _` | | | |  _ \
 *             |  __/\ V /| | || (_| | |_| | |_) |
 *              \___| \_/ |_|\__\__,_|____/|____/
 *
 *   Copyright (c) 2023
 *
 *   Licensed under the Business Source License, Version 1.1 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://github.com/FgForrest/evitaDB/blob/main/LICENSE
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.evitadb.test;

import io.evitadb.api.utils.Assert;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * This interface allows unit tests to easily prepare test directory, test file and also clean it up.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface TestFileSupport {

	/**
	 * Removes test directory with its contents.
	 */
	default void cleanTestDirectory() throws IOException {
		// clear Evita DB directory
		FileUtils.deleteDirectory(getTestDirectory().toFile());
	}

	/**
	 * Removes test directory with its contents.
	 */
	default void cleanTestSubDirectory(String directory) throws IOException {
		// clear Evita DB directory
		FileUtils.deleteDirectory(getTestDirectory().resolve(directory).toFile());
	}

	/**
	 * Removes test directory with its contents.
	 */
	default void cleanTestDirectoryWithRethrow() {
		try {
			cleanTestDirectory();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot empty target directory!", e);
		}
	}

	/**
	 * Removes test directory with its contents.
	 */
	default void cleanTestSubDirectoryWithRethrow(String directory) {
		try {
			cleanTestSubDirectory(directory);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot empty target directory!", e);
		}
	}

	/**
	 * Removes test directory with its contents and creates new empty directory on its place.
	 */
	default void prepareEmptyTestDirectory() throws IOException {
		cleanTestDirectory();
		final File dirFile = getTestDirectory().toFile();
		dirFile.mkdirs();
		Assert.isTrue(dirFile.exists() && dirFile.isDirectory(), "Target directory cannot be created!");
	}

	/**
	 * Returns path to the test directory.
	 */
	default Path getTestDirectory() {
		final String externallyDefinedPath = System.getProperty("evitaData");
		final Path dataPath;
		if (externallyDefinedPath == null) {
			dataPath = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "evita" + File.separator);
		} else {
			dataPath = Path.of(externallyDefinedPath);
		}
		return dataPath;
	}

	/**
	 * Returns path to the file with specified name in the test directory.
	 */
	default Path getPathInTargetDirectory(String fileName) {
		return getTestDirectory().resolve(fileName);
	}

	/**
	 * Returns file reference to the file with specified name in the test directory.
	 */
	default File createFileInTargetDirectory(String fileName) {
		return getPathInTargetDirectory(fileName).toFile();
	}

}
