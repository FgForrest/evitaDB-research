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

package io.evitadb.api.utils;

import io.evitadb.api.exception.InvalidFileNameException;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Locale;

/**
 * FileUtils contains various utility methods for work with file system.
 *
 * We know these functions are available in Apache Commons, but we try to keep our transitive dependencies as low as
 * possible so we rather went through duplication of the code.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FileUtils {

	private FileUtils() {}

	/**
	 * Creates safe file name (without extension part) and checks whether it has any valid size.
	 * @param fileName
	 * @return
	 */
	public static String formatFileName(@Nonnull Serializable fileName) {
		final String sanitizedForm = StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(
			fileName.toString().toLowerCase(Locale.ROOT).trim(), '-', ""
		);
		Assert.isTrue(
			sanitizedForm.length() > 0 && !"-".equals(sanitizedForm),
			() -> new InvalidFileNameException("Name " + fileName + " cannot be safely converted to ASCII characters for creating target file name!")
		);
		return sanitizedForm;
	}

	/**
	 * Returns true if directory has at least one file.
	 * @param directory
	 * @return
	 */
	public static boolean hasAnyFile(Path directory) {
		final File file = directory.toFile();
		if (file.exists()) {
			final File[] files = file.listFiles(File::isFile);
			return files != null && files.length > 0;
		}
		return false;
	}
}
