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

package io.evitadb.storage.model.memTable;

import io.evitadb.storage.MemTable;
import io.evitadb.storage.model.storageParts.entity.*;
import io.evitadb.storage.model.storageParts.index.*;
import io.evitadb.storage.model.storageParts.schema.EntitySchemaContainer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This enum contains all supported classes that can be stored in {@link MemTable}. This
 * enum is used for translating full Class to a small number to minimize memory overhead.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public enum MemTableRecordType {

	ENTITY_SCHEMA(EntitySchemaContainer.class),
	ENTITY(EntityBodyStoragePart.class),
	ATTRIBUTES(AttributesStoragePart.class),
	ASSOCIATED_DATA(AssociatedDataStoragePart.class),
	PRICES(PricesStoragePart.class),
	REFERENCES(ReferencesStoragePart.class),
	ENTITY_INDEX(EntityIndexStoragePart.class),
	ATTRIBUTE_UNIQUE_INDEX(UniqueIndexStoragePart.class),
	ATTRIBUTE_FILTER_INDEX(FilterIndexStoragePart.class),
	ATTRIBUTE_SORT_INDEX(SortIndexStoragePart.class),
	PRICE_LIST_CURRENCY_SUPER_INDEX(PriceListAndCurrencySuperIndexStoragePart.class),
	PRICE_LIST_CURRENCY_REF_INDEX(PriceListAndCurrencyRefIndexStoragePart.class),
	HIERARCHY_INDEX(HierarchyIndexStoragePart.class),
	FACET_INDEX(FacetIndexStoragePart.class);

	private static final Map<Class<? extends Serializable>, Byte> LOOKUP_INDEX;

	static {
		LOOKUP_INDEX = Arrays.stream(MemTableRecordType.values())
			.collect(
				Collectors.toMap(
					MemTableRecordType::getRecordType,
					it -> (byte) it.ordinal()
				)
			);
	}

	@Getter private final Class<? extends Serializable> recordType;

	/**
	 * Returns real type for the passes record type id.
	 */
	public static Class<? extends Serializable> typeFor(byte id) {
		return MemTableRecordType.values()[id].getRecordType();
	}

	/**
	 * Returns record type id for passed class.
	 */
	public static byte idFor(Class<? extends Serializable> type) {
		return Optional.ofNullable(LOOKUP_INDEX.get(type))
			.orElseThrow(() -> new IllegalArgumentException("Type " + type + " cannot be handled by MemTable!"));
	}

}
