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

import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.EntityReferenceContract;
import io.evitadb.api.exception.UnexpectedResultException;
import io.evitadb.api.io.EvitaResponseExtraResult;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.api.query.filter.WithinRootHierarchy;
import io.evitadb.api.query.require.EntityContentRequire;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This DTO contains hierarchical structure of entities referenced by the entities required by the query. It copies
 * hierarchical structure of those entities and contains their identification or full body as well as information on
 * cardinality of referencing entities.
 *
 * For example when we need to render menu for entire e-commerce site, but we want to take excluded subtrees into
 * an account and also reflect the filtering conditions that may filter out dozens of products (and thus leading to
 * empty categories) we can invoke following query:
 *
 * <pre>
 * query(
 *     entities('PRODUCT'),
 *     filterBy(
 *         and(
 *             eq('visible', true),
 *             inRange('valid', 2020-07-30T20:37:50+00:00),
 *             priceInCurrency('USD'),
 *             priceValidIn(2020-07-30T20:37:50+00:00),
 *             priceInPriceLists('vip', 'standard'),
 *             withinRootHierarchy('CATEGORY', excluding(3, 7))
 *         )
 *     ),
 *     require(
 *         page(1, 20),
 *         hierarchyStatistics('CATEGORY', entityBody(), attributes())
 *     )
 * )
 * </pre>
 *
 * This query would return first page with 20 products (omitting hundreds of others on additional pages) but also
 * returns a HierarchyStatistics in additional data. Statistics respect hierarchical constraints specified in the filter
 * of the query. In our example sub-trees with ids 3 and 7 will be omitted from the statistics.
 *
 * This object may contain following structure:
 *
 * <pre>
 * Electronics -> 1789
 *     TV -> 126
 *         LED -> 90
 *         CRT -> 36
 *     Washing machines -> 190
 *         Slim -> 40
 *         Standard -> 40
 *         With drier -> 23
 *         Top filling -> 42
 *         Smart -> 45
 *     Cell phones -> 350
 *     Audio / Video -> 230
 *     Printers -> 80
 * </pre>
 *
 * The tree will contain category entities loaded with `attributes` instead the names you see in the example. The number
 * after the arrow represents the count of the products that are referencing this category (either directly or some of its
 * children). You can see there are only categories that are valid for the passed query - excluded category subtree will
 * not be part of the category listing (query filters out all products with excluded category tree) and there is also no
 * category that happens to be empty (e.g. contains no products or only products that don't match the filter constraint).
 *
 * TOBEDONE JNO - consider extension so that stats are computed either for full filter and also for base line without user filter
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class HierarchyStatistics implements EvitaResponseExtraResult {
	private static final long serialVersionUID = -5337743162562869243L;
	/**
	 * Index holds the statistics for particular hierarchy entity types.
	 * Key is the identification of the entity type, value contains list of statistics for the single level (probably
	 * root or whatever is filtered by the query) of the hierarchy entity.
	 */
	private final Map<Serializable, List<LevelInfo<? extends Serializable>>> statistics;

	/**
	 * Method returns the cardinality statistics for the top most level of referenced hierarchical entities.
	 * Level is either the root level if {@link WithinRootHierarchy} constraint or no hierarchical filtering constraint
	 * was used at all. Or it's the level requested by {@link WithinHierarchy} constraint.
	 */
	@Nullable
	public <T extends Serializable> List<LevelInfo<T>> getStatistics(@Nonnull Serializable entityType, @Nonnull Class<T> expectedType) {
		final List<LevelInfo<?>> statisticsByType = statistics.get(entityType);
		if (!statisticsByType.isEmpty()) {
			// checks if first existing level info entity has expected type
			statisticsByType.stream()
				.filter(Objects::nonNull)
				.map(LevelInfo::getEntity)
				.findFirst()
				.ifPresent(entity -> {
					if (!expectedType.equals(entity.getClass())) {
						throw new UnexpectedResultException(expectedType, entity.getClass());
					}
				});
		}
		//noinspection unchecked
		return statisticsByType.stream()
			.map(it -> (LevelInfo<T>) it)
			.collect(Collectors.toList());
	}

	@Override
	public int hashCode() {
		return Objects.hash(statistics);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final HierarchyStatistics that = (HierarchyStatistics) o;

		for (Entry<Serializable, List<LevelInfo<? extends Serializable>>> entry : statistics.entrySet()) {
			final List<LevelInfo<? extends Serializable>> stats = entry.getValue();
			final List<LevelInfo<? extends Serializable>> otherStats = that.statistics.get(entry.getKey());

			if (stats.size() != otherStats.size()) {
				return false;
			}

			for (int i = 0; i < stats.size(); i++) {
				final LevelInfo<? extends Serializable> levelInfo = stats.get(i);
				final LevelInfo<? extends Serializable> otherLevelInfo = otherStats.get(i);

				if (!levelInfo.equals(otherLevelInfo)) {
					return false;
				}
			}

		}

		return true;
	}

	@Override
	public String toString() {
		final StringBuilder treeBuilder = new StringBuilder();
		for (Map.Entry<Serializable, List<LevelInfo<?>>> statisticsByType : statistics.entrySet()) {
			treeBuilder.append(statisticsByType.getKey().toString()).append(System.lineSeparator());

			for (LevelInfo<?> levelInfo : statisticsByType.getValue()) {
				appendLevelInfoTreeString(treeBuilder, levelInfo, 1);
			}
		}
		return treeBuilder.toString();
	}

	/**
	 * Creates string representation of subtree of passed level info
	 *
	 * @param treeBuilder  string builder to which the string will be appended to
	 * @param levelInfo    level info to render
	 * @param currentLevel level on which passed level info is being placed
	 */
	private void appendLevelInfoTreeString(@Nonnull StringBuilder treeBuilder, @Nonnull LevelInfo<?> levelInfo, int currentLevel) {
		treeBuilder.append("    ".repeat(currentLevel))
			.append(levelInfo)
			.append(System.lineSeparator());

		for (LevelInfo<?> child : levelInfo.getChildrenStatistics()) {
			appendLevelInfoTreeString(treeBuilder, child, currentLevel + 1);
		}
	}

	/**
	 * This DTO represents single hierarchical entity in the statistics tree. It contains identification of the entity,
	 * the cardinality of queried entities that refer to it and information about children level.
	 */
	@Data
	@EqualsAndHashCode
	public static final class LevelInfo<T extends Serializable> implements Comparable<LevelInfo<T>> {
		/**
		 * Hierarchical entity identification - it may be {@link Integer} representing primary key of the entity if no
		 * {@link EntityContentRequire} requirements were passed within {@link io.evitadb.api.query.require.HierarchyStatistics}
		 * constraint, or it may be rich {@link io.evitadb.api.data.SealedEntity} object if the richer requirements were specified.
		 */
		private final T entity;
		/**
		 * Contains the number of queried entities that refer directly to this {@link #entity} or to any of its children
		 * entities.
		 */
		private final int cardinality;
		/**
		 * Contains statistics of the entities that are subordinate (children) of this {@link #entity}.
		 */
		@Nonnull
		private final List<LevelInfo<T>> childrenStatistics;

		@Override
		public String toString() {
			return "[" + cardinality + "] " + entity;
		}

		@Override
		public int compareTo(LevelInfo<T> other) {
			if (entity instanceof EntityReferenceContract) {
				return Integer.compare(((EntityReferenceContract<?>) entity).getPrimaryKey(), ((EntityReferenceContract<?>) other.entity).getPrimaryKey());
			} else if (entity instanceof EntityContract) {
				return Integer.compare(((EntityContract) entity).getPrimaryKey(), ((EntityContract) other.entity).getPrimaryKey());
			} else {
				throw new IllegalArgumentException("Cannot compare: " + entity.getClass());
			}
		}

	}

}
