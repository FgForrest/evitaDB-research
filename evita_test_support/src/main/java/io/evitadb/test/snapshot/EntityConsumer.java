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

package io.evitadb.test.snapshot;

import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;

/**
 * This interface is used to consume {@link io.evitadb.api.data.structure.Entity} read through {@link GenericSerializedCatalogReader}
 * from previously stored database snapshot.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface EntityConsumer {

	/**
	 * This method is called only once per entity collection type.
	 */
	void setup(MutableCatalogEntityHeader header, EntitySchema schema);

	/**
	 * Processes single entity read from the database snapshot
	 *
	 * @param schema - entity schema
	 * @param entity - entity itself
	 * @return false when next entity should be read even if it's in the queue
	 */
	boolean accept(EntitySchema schema, Entity entity);

	/**
	 * This method is called only once when collection is completely read.
	 */
	void close();

}
