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

package io.evitadb.api.query.filter;

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;

/**
 * This `withinRootHierarchy` constraint accepts [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
 * entity type in first argument. There are also optional second and third arguments - see optional arguments {@link DirectRelation},
 * and {@link Excluding}.
 *
 * Function returns true if entity has at least one [reference](../model/entity_model.md#references) that relates to specified entity
 * type and entity either directly or relates to any other entity of the same type with [hierarchical placement](../model/entity_model.md#hierarchical-placement)
 * subordinate to the directly related entity placement (in other words is present in its sub-tree).
 *
 * Let's have following hierarchical tree of categories (primary keys are in brackets):
 *
 * - TV (1)
 *     - Crt (2)
 *     - LCD (3)
 *         - big (4)
 *         - small (5)
 *     - Plasma (6)
 * - Fridges (7)
 *
 * When constraint `withinRootHierarchy('category')` is used in a query targeting product entities all products that
 * relates to any of categories will be returned.
 *
 * Only single `withinRootHierarchy` constraint can be used in the query.
 *
 * Example:
 *
 * ```
 * withinRootHierarchy('category')
 * ```
 *
 * If you want to constraint the entity that you're querying on you can also omit entity type specification. See example:
 *
 * ```
 * query(
 *    entities('CATEGORY'),
 *    filterBy(
 *       withinRootHierarchy()
 *    )
 * )
 * ```
 *
 * This query will return all categories within `CATEGORY` entity.
 *
 * You may use this constraint to list entities that refers to the hierarchical entities:
 *
 * ```
 * query(
 *    entities('PRODUCT'),
 *    filterBy(
 *       withinRootHierarchy('CATEGORY')
 *    )
 * )
 * ```
 *
 * This query returns all products that are attached to any category.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class WithinRootHierarchy extends AbstractFilterConstraintContainer implements HierarchyFilterConstraint {
	private static final long serialVersionUID = -4396541048481960654L;

	private WithinRootHierarchy(Serializable[] argument, FilterConstraint[] fineGrainedConstraints) {
		super(argument, fineGrainedConstraints);
		checkInnerConstraintValidity(fineGrainedConstraints);
	}

	public WithinRootHierarchy(HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		super(fineGrainedConstraints);
		checkInnerConstraintValidity(fineGrainedConstraints);
	}

	public WithinRootHierarchy(Serializable entityType, HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		super(entityType, fineGrainedConstraints);
		checkInnerConstraintValidity(fineGrainedConstraints);
	}

	private void checkInnerConstraintValidity(FilterConstraint[] fineGrainedConstraints) {
		for (FilterConstraint filterConstraint : fineGrainedConstraints) {
			Assert.isTrue(
				filterConstraint instanceof Excluding ||
				(filterConstraint instanceof DirectRelation && getEntityType() == null),
				"Constraint withinRootHierarchy accepts only Excluding, or DirectRelation when it targets same entity type as inner constraint!"
			);
		}
	}

	/**
	 * Returns name of the entity this hierarchy constraint relates to.
	 * Returns null if entity type is not specified and thus the same entity type as "queried" should be used.
	 * @return
	 */
	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T getEntityType() {
		final Serializable[] arguments = getArguments();
		final Serializable firstArgument = arguments.length > 0 ? arguments[0] : null;
		//noinspection unchecked
		return firstArgument instanceof Integer ? null : (T) firstArgument;
	}

	/**
	 * Returns true if withinHierarchy should return only entities directly related to the root entity.
	 * @return
	 */
	@Override
	public boolean isDirectRelation() {
		return Arrays.stream(getConstraints())
			.anyMatch(DirectRelation.class::isInstance);
	}

	/**
	 * Returns ids of child entities which hierarchies should be excluded from search.
	 */
	@Override
	@Nonnull
	public int[] getExcludedChildrenIds() {
		return Arrays.stream(getConstraints())
				.filter(Excluding.class::isInstance)
				.map(it -> ((Excluding) it).getPrimaryKeys())
				.findFirst()
				.orElseGet(() -> new int[0]);
	}

	@Override
	public FilterConstraint getCopyWithNewChildren(FilterConstraint[] innerConstraints) {
		return new WithinRootHierarchy(getArguments(), innerConstraints);
	}

	@Override
	public boolean isNecessary() {
		return super.isNecessary() || isApplicable();
	}

	@Override
	public boolean isApplicable() {
		final Serializable[] arguments = getArguments();
		return arguments.length >= 1 || getConstraints().length >= 1;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new WithinRootHierarchy(newArguments, getConstraints());
	}
}
