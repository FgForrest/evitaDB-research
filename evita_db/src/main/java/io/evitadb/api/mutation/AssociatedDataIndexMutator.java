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

package io.evitadb.api.mutation;

import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.IndexType;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Locale;

import static io.evitadb.api.mutation.AttributeIndexMutator.entityHasEffectivelyLocalizedValueExcept;
import static java.util.Optional.ofNullable;

/**
 * This interface is used to co-locate attribute mutating routines which are rather procedural and long to avoid excessive
 * amount of code in {@link EntityIndexLocalMutationExecutor}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface AssociatedDataIndexMutator {

	/**
	 * Handles {@link io.evitadb.api.data.mutation.associatedData.UpsertAssociatedDataMutation} and alters {@link EntityIndex}
	 * data according this mutation.
	 */
	static void executeAssociatedDataUpsert(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex index,
		@Nonnull AssociatedDataKey associatedDataKey
	) {
		ofNullable(associatedDataKey.getLocale())
			.ifPresent(it -> executor.upsertEntityLanguage(index, it));
	}

	/**
	 * Handles {@link io.evitadb.api.data.mutation.associatedData.RemoveAssociatedDataMutation} and alters {@link EntityIndex}
	 * data according this mutation.
	 */
	static <T extends Serializable & Comparable<T>> void executeAssociatedDataRemoval(
		@Nonnull EntityIndexLocalMutationExecutor executor,
		@Nonnull EntityIndex index,
		@Nonnull AssociatedDataKey associatedDataKey
	) {
		final Locale locale = associatedDataKey.getLocale();
		if (locale != null) {
			final int entityPrimaryKey = executor.getPrimaryKeyToIndex(IndexType.ENTITY_INDEX);
			if (!entityHasEffectivelyLocalizedValueExcept(executor, entityPrimaryKey, locale, null, associatedDataKey.getAssociatedDataName())) {
				executor.removeEntityLanguage(index, locale);
			}
		}
	}

}
