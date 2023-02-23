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
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.mutation.Mutation;
import io.evitadb.api.data.structure.AssociatedData;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Associated data {@link Mutation} allows to execute mutation operations on {@link AssociatedData} of the {@link Entity}
 * object. Each associated data change is increases {@link AssociatedDataValue#getVersion()} by one, associated data removal only sets
 * tombstone flag on a associated data value and doesn't really remove it. Possible removal will be taken care of during
 * compaction process, leaving associatedDatas in place allows to see last assigned value to the associated data and also consult
 * last version of the associated data.
 *
 * These traits should help to manage concurrent transactional process as updates to the same entity could be executed
 * safely and concurrently as long as associated data modification doesn't overlap. Some mutations may also overcome same
 * associated data concurrent modification if it's safely additive (i.e. incrementation / decrementation and so on).
 *
 * Exact mutations also allows engine implementation to safely update only those indexes that the change really affects
 * and doesn't require additional analysis.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class AssociatedDataMutation implements LocalMutation<AssociatedDataValue, AssociatedDataKey> {
	private static final long serialVersionUID = 2877681453791825337L;
	@Getter protected final AssociatedDataKey associatedDataKey;

	protected AssociatedDataMutation(AssociatedDataKey associatedDataKey) {
		Assert.isTrue(associatedDataKey != null, "Associated data key cannot be null for set associated data mutation!");
		this.associatedDataKey = associatedDataKey;
	}

	@Override
	public @Nonnull
	Class<AssociatedDataValue> affects() {
		return AssociatedDataValue.class;
	}

	@Override
	public AssociatedDataKey getComparableKey() {
		return associatedDataKey;
	}

	@Nonnull
	public EntitySchema verifyOrEvolveSchema(
		@Nonnull EntitySchema schema,
		@Nullable AssociatedDataSchema associatedDataSchema,
		@Nonnull Serializable associatedDataValue,
		@Nonnull Supplier<EntitySchema> schemaEvolutionApplicator
	) throws InvalidMutationException {
		// when associated data definition is known execute first encounter formal verification
		if (associatedDataSchema != null) {
			Assert.isTrue(
				associatedDataSchema.getType().isInstance(associatedDataValue),
				() -> new InvalidMutationException(
					"Invalid type: " + associatedDataValue.getClass() + "! " +
						"Associated data " + associatedDataKey.getAssociatedDataName() + " was already stored as type " + associatedDataSchema.getType() + ". " +
						"All values of associated data " + associatedDataKey.getAssociatedDataName() + " must respect this data type!"
				)
			);
			if (associatedDataSchema.isLocalized()) {
				Assert.isTrue(
					associatedDataKey.isLocalized(),
					() -> new InvalidMutationException(
						"Associated data " + associatedDataKey.getAssociatedDataName() + " was already stored as localized value. " +
							"All values of associated data " + associatedDataKey.getAssociatedDataName() + " must be localized now " +
							"- use different associated data name for locale independent variant of associated data!!"
					)
				);
			} else {
				Assert.isTrue(
					!associatedDataKey.isLocalized(),
					() -> new InvalidMutationException(
						"Associated data " + associatedDataKey.getAssociatedDataName() + " was not stored as localized value. " +
							"No values of associated data " + associatedDataKey.getAssociatedDataName() + " can be localized now " +
							"- use different associated data name for localized variant of associated data!"
					)
				);
			}
			return schema;
			// else check whether adding associatedDatas on the fly is allowed
		} else if (schema.allows(EvolutionMode.ADDING_ASSOCIATED_DATA)) {
			// evolve schema automatically
			return schemaEvolutionApplicator.get();
		} else {
			throw new InvalidMutationException(
				"Unknown associated data " + associatedDataKey.getAssociatedDataName() + "! You must first alter entity schema to be able to add this associated data to the entity!"
			);
		}
	}

}
