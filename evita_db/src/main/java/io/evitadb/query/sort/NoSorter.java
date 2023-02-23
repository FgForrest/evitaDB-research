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

package io.evitadb.query.sort;

import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.Formula;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This implementation of {@link Sorter} doesn't sort but just slices the formula output. The result ids are sorted
 * naturally - ie. from smallest to greatest id.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class NoSorter implements Sorter {
	public static final NoSorter INSTANCE = new NoSorter();
	private static final int[] EMPTY_RESULT = new int[0];

	private NoSorter() {
	}

	@Nonnull
	@Override
	public Sorter andThen(Sorter sorterForUnknownRecords) {
		throw new UnsupportedOperationException("NoSorter cannot be chained!");
	}

	@Nullable
	@Override
	public Sorter getNextSorter() {
		return null;
	}

	@Override
	public int[] sortAndSlice(@Nonnull Formula input, int startIndex, int endIndex) {
		final Bitmap results = input.compute();
		final int maxLength = Math.min(endIndex - startIndex, results.size() - startIndex);
		return endIndex > 0 && !results.isEmpty() ? results.getRange(startIndex, startIndex + maxLength) : EMPTY_RESULT;
	}

}