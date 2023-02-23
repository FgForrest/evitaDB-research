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

package io.evitadb.storage.model.storageParts.schema;

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.storage.model.storageParts.StoragePart;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Storage part envelops {@link EntitySchema}. Storage part has always id fixed to 1 because there is no other schema
 * in the entity collection than this one.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class EntitySchemaContainer implements StoragePart {
	private static final long serialVersionUID = -1973029963787048578L;
	@Getter private final EntitySchema entitySchema;

	@Nullable
	@Override
	public Long getUniquePartId() {
		return 1L;
	}

	@Override
	public long computeUniquePartIdAndSet(@Nonnull KeyCompressor keyCompressor) {
		return 1L;
	}

	@Override
	public String toString() {
		return "EntitySchemaContainer{" +
			"schema=" + entitySchema.getName() +
			'}';
	}
}
