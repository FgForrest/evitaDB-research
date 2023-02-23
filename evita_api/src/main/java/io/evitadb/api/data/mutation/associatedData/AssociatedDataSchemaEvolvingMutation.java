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

package io.evitadb.api.data.mutation.associatedData;

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataValue;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.function.UnaryOperator;

/**
 * Abstract parent for all associated data mutations that require schema validation / evolution.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class AssociatedDataSchemaEvolvingMutation extends AssociatedDataMutation implements SchemaEvolvingLocalMutation<AssociatedDataValue, AssociatedDataKey> {
	private static final long serialVersionUID = -1200943946647440138L;

	protected AssociatedDataSchemaEvolvingMutation(@Nonnull AssociatedDataKey associatedDataKey) {
		super(associatedDataKey);
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return associatedDataKey;
	}

	@Override
	public @Nonnull
	EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		return verifyOrEvolveSchema(
			schema,
			schema.getAssociatedData(associatedDataKey.getAssociatedDataName()),
			getAssociatedDataValue(),
			() -> {
				final EntitySchemaBuilder schemaBuilder = schema.open(schemaUpdater);
				if (associatedDataKey.isLocalized()) {
					schemaBuilder.withLocale(associatedDataKey.getLocale());
				}
				return schemaBuilder
					.withAssociatedData(
						associatedDataKey.getAssociatedDataName(),
						getAssociatedDataValue().getClass(),
						whichIs -> whichIs.localized(associatedDataKey::isLocalized)
					)
					.applyChanges();
			}
		);
	}

	public abstract Serializable getAssociatedDataValue();

}
