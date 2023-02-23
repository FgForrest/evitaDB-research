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

package io.evitadb.api.io.extraResult;

import io.evitadb.api.exception.UnexpectedResultException;
import io.evitadb.api.io.EvitaResponseExtraResult;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * This DTO class contains information about full parent paths of hierarchical entities the requested entity is referencing.
 * Information can is usually used when rendering breadcrumb path for the entity.
 *
 * Instance of this class is returned in {@link io.evitadb.api.io.EvitaResponseBase#getAdditionalResults(Class)} when
 * {@link io.evitadb.api.query.require.Parents} require constraint is used in the query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class Parents implements EvitaResponseExtraResult {
	private static final long serialVersionUID = -1205579626554971896L;
	private final Map<Serializable, ParentsByType<?>> parentsIndex;

	/**
	 * Returns DTO that contains information of parents of particular referenced entity type.
	 * When `product` entity is related to hierarchical entity `category`, calling this method when requesting products
	 * will provide information about category paths for each product.
	 */
	@Nullable
	public <T extends Serializable> ParentsByType<T> ofType(@Nonnull Serializable entityType, @Nonnull Class<T> expectedType) {
		final ParentsByType<?> parentsByType = parentsIndex.get(entityType);
		if (!parentsByType.parentsForEntity.isEmpty()) {
			// checks if first existing array of parents has expected type
			parentsByType.parentsForEntity.values().stream()
					.filter(Objects::nonNull)
					.flatMap(map -> map.values().stream())
					.filter(Objects::nonNull)
					.findFirst()
					.ifPresent(parent -> {
						if (!expectedType.equals(parent.getClass().getComponentType())) {
							throw new UnexpectedResultException(expectedType, parent.getClass().getComponentType());
						}
					});
		}
		//noinspection unchecked
		return (ParentsByType<T>) parentsByType;
	}

	/**
	 * This DTO contains information about ancestor paths for related hierarchy entity.
	 */
	@RequiredArgsConstructor
	public static class ParentsByType<T extends Serializable> {
		private static final Integer[] EMPTY_INTEGERS = new Integer[0];
		/**
		 * Contains the type of the entity that is referenced.
		 */
		private final Serializable entityType;
		/**
		 * Contains index of entity PKs (key) related to map of parent chains. There may be multiple chains if the entity
		 * is related to multiple hierarchical entities of the same type.
		 */
		private final Map<Integer, Map<Integer, T[]>> parentsForEntity;

		/**
		 * Returns primary keys of the all parents of entity with passed primary key.
		 * If product `Red gloves (id=5)` is referencing category `Gloves (id=87)`, that has parent category `Winter (id=124)`,
		 * that also has parent category `Clothes (id=45)`, then calling `getParentsFor(5)` would return array of integers:
		 * [45, 124, 87]
		 *
		 * Response represents entire category hierarchy path from root down through its parent-child chain
		 * to the referenced entity.
		 *
		 * @throws IllegalArgumentException when the entity relates to two or more entities of this type (eg. is referencing
		 *                                  two or more categories in our example), if it's possible use method {@link #getParentsFor(int, int)} instead
		 */
		@Nullable
		public T[] getParentsFor(int primaryKey) throws IllegalArgumentException {
			final Map<Integer, T[]> result = parentsForEntity.get(primaryKey);
			if ((result == null) || result.isEmpty()) {
				return null;
			} else if (result.size() == 1) {
				return result.values().iterator().next();
			} else {
				throw new IllegalArgumentException("There are " + result.size() + " relations for entity type " + entityType + " with id " + primaryKey + "!");
			}
		}

		/**
		 * Returns primary keys of all referenced entities passed entity id is related to. This method is handy for using
		 * method {@link #getParentsFor(int, int)} when we need to find out which referenced id should be requested.
		 */
		@Nullable
		public Integer[] getReferencedEntityIds(int primaryKey) {
			return ofNullable(parentsForEntity.get(primaryKey))
				.map(it -> it.keySet().toArray(EMPTY_INTEGERS))
				.orElse(null);
		}

		/**
		 * Returns primary keys of the all parents of entity with combination passed primary key and referenced entity primary key.
		 * If product `Red gloves (id=5)` is referencing category `Gloves (id=87)`, that has parent category `Winter (id=124)`,
		 * that also has parent category `Clothes (id=45)`, then calling `getParentsFor(5, 87)` would return array of integers:
		 * [45, 124, 87]
		 *
		 * Response represents entire category hierarchy path from root down through its parent-child chain
		 * to the referenced entity.
		 */
		@Nullable
		public T[] getParentsFor(int primaryKey, int referencedId) {
			return ofNullable(parentsForEntity.get(primaryKey))
				.map(it -> it.get(referencedId))
				.orElse(null);
		}
	}

}
