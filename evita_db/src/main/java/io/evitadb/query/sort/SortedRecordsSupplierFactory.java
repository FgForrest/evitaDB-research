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

import javax.annotation.Nonnull;

/**
 * Provides access to presorted arrays of records according to certain attribute or other data value.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public interface SortedRecordsSupplierFactory {

	/**
	 * Creates sorted records supplier for ascending order.
	 */
	@Nonnull
	SortedRecordsSupplier getAscendingOrderRecordsSupplier();

	/**
	 * Creates sorted records supplier for descending order.
	 */
	@Nonnull
	SortedRecordsSupplier getDescendingOrderRecordsSupplier();

	/**
	 * Provides access to sorted records array.
	 */
	interface SortedRecordsSupplier {

		/**
		 * Returns count of all records of the supplier.
		 */
		int getRecordCount();

		/**
		 * Returns bitmap of all record ids present in the sort supplier in distinct ascending order.
		 * Example: 1, 3, 4, 6, 8, 12
		 */
		Bitmap getAllRecords();

		/**
		 * Contains index of record from {@link #getAllRecords()} in {@link #getSortedRecordIds()} array.
		 * Example: 1, 4, 5, 0, 3, 2
		 */
		int[] getRecordPositions();

		/**
		 * Returns array of records in "sorted" order - i.e. order that conforms to the referring {@link Comparable} order.
		 * Example: 6, 1, 12, 8, 3, 4
		 */
		int[] getSortedRecordIds();

	}


}