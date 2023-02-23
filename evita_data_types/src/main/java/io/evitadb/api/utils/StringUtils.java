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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * String utils contains shared utility methods for working with Strings.
 * We know these functions are available in Apache Commons, but we try to keep our transitive dependencies as low as
 * possible, so we rather went through duplication of the code.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {
	/**
	 * The number of bytes in a kilobyte.
	 */
	public static final int ONE_KB = 1024;
	/**
	 * The number of bytes in a megabyte.
	 */
	public static final int ONE_MB = ONE_KB * ONE_KB;
	/**
	 * The number of bytes in a gigabyte.
	 */
	public static final int ONE_GB = ONE_KB * ONE_MB;

	/**
	 * Displays bytes in human-readable form (i.e. using shortening for kB, MB, GB and so on).
	 */
	@Nonnull
	public static String formatByteSize(int sizeInBytes) {
		if (sizeInBytes / ONE_GB > 0) {
			return String.format("%.2f GB", (double)sizeInBytes / ONE_GB );
		} else if (sizeInBytes / ONE_MB > 0) {
			return String.format("%.2f MB", (double)sizeInBytes / ONE_MB );
		} else if (sizeInBytes / ONE_KB > 0) {
			return sizeInBytes / ONE_KB + " KB";
		} else {
			return sizeInBytes + " B";
		}
	}

	/**
	 * Displays bytes in human-readable form (i.e. using shortening for kB, MB, GB and so on).
	 */
	@Nonnull
	public static String formatByteSize(long sizeInBytes) {
		if (sizeInBytes / ONE_GB > 0) {
			return String.format("%.2f GB", (double)sizeInBytes / ONE_GB );
		} else if (sizeInBytes / ONE_MB > 0) {
			return String.format("%.2f MB", (double)sizeInBytes / ONE_MB );
		} else if (sizeInBytes / ONE_KB > 0) {
			return sizeInBytes / ONE_KB + " KB";
		} else {
			return sizeInBytes + " B";
		}
	}

	/**
	 * Displays high number (count) in human-readable form (i.e. using shortening for thousands, millions and so on).
	 */
	@Nonnull
	public static String formatCount(int count) {
		if (count / 1_000_000_000 > 0) {
			return String.format("%.2f bil.", (double)count / 1_000_000_000 );
		} else if (count / 100_000 > 0) {
			return String.format("%.2f mil.", (double)count / 1_000_000 );
		} else if (count / 1_000 > 0) {
			return String.format("%.2f thousands", (double)count / 1_000 );
		} else {
			return String.valueOf(count);
		}
	}

	/**
	 * Displays high number (count) in human-readable form (i.e. using shortening for thousands, millions and so on).
	 */
	@Nonnull
	public static String formatCount(long count) {
		if (count / 1_000_000_000 > 0) {
			return String.format("%.2f bil.", (double)count / 1_000_000_000 );
		} else if (count / 100_000 > 0) {
			return String.format("%.2f mil.", (double)count / 1_000_000 );
		} else if (count / 1_000 > 0) {
			return String.format("%.2f thousands", (double)count / 1_000 );
		} else {
			return String.valueOf(count);
		}
	}

	/**
	 * Formats value in nanoseconds (used for measuring elapsed time) to human readable format.
	 * Nanoseconds are omitted when at least second is printed.
	 */
	public static String formatNano(long nanoSeconds) {
		return formatNano(nanoSeconds, true);
	}

	/**
	 * Formats value in nanoseconds (used for measuring elapsed time) to human readable format.
	 * Nanoseconds are not omitted and printed every time - even if nano spans days.
	 */
	public static String formatPreciseNano(long nanoSeconds) {
		return formatNano(nanoSeconds, false);
	}

	/**
	 * Lowercases first character of the string.
	 */
	public static String uncapitalize(String string) {
		if (string == null || string.length() == 0) {
			return string;
		}

		char firstCharacter = Character.toLowerCase(string.charAt(0));
		if (firstCharacter == string.charAt(0)) {
			return string;
		}

		char[] chars = string.toCharArray();
		chars[0] = firstCharacter;
		return new String(chars, 0, chars.length);
	}

	/**
	 * Removes national diacritic characters and replaces them with ASCII characters.
	 * equivalents.
	 */
	@Nonnull
	public static String removeDiacritics(@Nonnull String original) {
		final String normalizedResult = Normalizer.normalize(original, Form.NFKD);
		return normalizedResult.replaceAll("[^\\p{ASCII}]", "");
	}

	/**
	 * Removes national diacritic characters and replaces them with ASCII equivalents. Finally removes all non alphanumeric
	 * characters (respecting exceptions) and replaces them with
	 *
	 * @param charactersToKeep in regular expression form
	 */
	@Nonnull
	public static String removeDiacriticsAndAllNonStandardCharactersExcept(@Nonnull String original, char sanitizeCharacter, @Nonnull String charactersToKeep) {
		final String asciiName = removeDiacritics(original);
		final String sanitizeString = String.valueOf(sanitizeCharacter);
		final String quotedSanitizeString = Pattern.quote(sanitizeString);
		return asciiName.replaceAll("[^A-Za-z0-9" + charactersToKeep + "]", sanitizeString)
			.replaceAll(quotedSanitizeString + "+", sanitizeString)
			.replaceAll("\\A" + quotedSanitizeString + "+", "")
			.replaceAll(quotedSanitizeString + "+\\z", "");
	}

	/**
	 * Computes and returns requests per second rounded to two decimal places.
	 */
	public static String formatRequestsPerSec(int requestsProcessed, long nanoSeconds) {
		if (requestsProcessed == 0) {
			return "N/A";
		} else {
			final double reqsSec = requestsProcessed / ((double) nanoSeconds / (double) 1_000_000_000);
			return new BigDecimal(String.valueOf(reqsSec)).setScale(2, RoundingMode.HALF_UP) + " reqs/s";
		}
	}

	/**
	 * Prints unknown object as human readable string.
	 */
	public static String unknownToString(@Nullable Object value) {
		if (value instanceof Object[]) {
			return Arrays.toString((Object[]) value);
		} else if (value != null) {
			return value.toString();
		} else {
			return "<NULL>";
		}
	}

	/*
		PRIVATE METHODS
	 */

	/**
	 * Method will repeat `padCharacters` to the start of `currentString` until `expectedLength` is reached.
	 */
	@Nonnull
	private static String leftPad(char padCharacter, int expectedLength, @Nonnull String currentString) {
		final int padCount = expectedLength - currentString.length();
		return padCount > 0 ? Strings.repeat(String.valueOf(padCharacter), padCount) + currentString : currentString;
	}

	/**
	 * Method will remove all zeroes at the end of the `string`.
	 */
	@Nonnull
	private static String stripTrailingZeroes(@Nonnull String string) {
		final char[] stringChars = string.toCharArray();
		for (int i = stringChars.length - 1; i >= 0; i--) {
			char character = stringChars[i];
			if (character != '0') {
				return string.substring(0, i + 1);
			}
		}
		return "";
	}

	/**
	 * Formats value in nanoseconds (used for measuring elapsed time) to human-readable format.
	 */
	private static String formatNano(long nanoSeconds, boolean omitNanoIfPossible) {
		final StringBuilder sb = new StringBuilder();
		long seconds = nanoSeconds / 1000000000;
		long days = seconds / (3600 * 24);
		appendIfNonZero(sb, days, "", "d");
		seconds -= (days * 3600 * 24);
		long hours = seconds / 3600;
		appendIfNonZero(sb, hours, "", "h");
		seconds -= (hours * 3600);
		long minutes = seconds / 60;
		appendIfNonZero(sb, minutes, "", "m");

		seconds -= (minutes * 60);
		if (!omitNanoIfPossible || (sb.length() == 0 && seconds == 0)) {
			long nanos = nanoSeconds % 1000000000;
			final String suffix = getIfNonZero(nanos, "." + "0".repeat(9 - String.valueOf(nanos).length()), "s");
			append(sb, seconds, "", suffix);
		} else {
			appendIfNonZero(sb, seconds, "", "s");
		}

		return sb.toString();
	}

	/**
	 * Appends `prefix`, `value` and suffix to the `sb` StringBuilder when `value` is greater than zero.
	 */
	private static void appendIfNonZero(@Nonnull StringBuilder sb, long value, @Nonnull String prefix, @Nonnull String suffix) {
		if (value > 0) {
			append(sb, value, prefix, suffix);
		}
	}

	/**
	 * Envelopes `value` with `prefix` and suffix when `value` is greater than zero.
	 */
	private static String getIfNonZero(long value, @Nonnull String prefix, @Nonnull String suffix) {
		final StringBuilder sb = new StringBuilder();
		if (value > 0) {
			append(sb, value, prefix, suffix);
		}
		return sb.toString();
	}

	/**
	 * Appends `prefix`, `value` and suffix to the `sb` StringBuilder.
	 */
	private static void append(@Nonnull StringBuilder sb, long value, @Nonnull String prefix, @Nonnull String suffix) {
		if (sb.length() > 0) {
			sb.append(" ");
		}
		sb.append(prefix).append(value).append(suffix);
	}

}
