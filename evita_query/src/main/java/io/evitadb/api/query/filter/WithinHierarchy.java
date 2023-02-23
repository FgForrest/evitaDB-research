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
 * This `withinHierarchy` constraint accepts [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
 * entity type in first argument, primary key of [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
 * type of entity with [hierarchical placement](../model/entity_model.md#hierarchical-placement) in second argument. There
 * are also optional third and fourth arguments - see optional arguments {@link DirectRelation}, {@link ExcludingRoot}
 * and {@link Excluding}.
 *
 * Constraint can also have only one numeric argument representing primary key of [Integer](https://docs.oracle.com/javase/8/docs/api/java/lang/Integer.html)
 * the very same entity type in case this entity has [hierarchical placement](../model/entity_model.md#hierarchical-placement)
 * defined. This format of the constraint may be used for example for returning category sub-tree (where we want to return
 * category entities and also constraint them by their own hierarchy placement).
 *
 * Function returns true if entity has at least one [reference](../model/entity_model.md#references) that relates to specified entity
 * type and entity either directly or relates to any other entity of the same type with [hierarchical placement](../model/entity_model.md#hierarchical-placement)
 * subordinate to the directly related entity placement (in other words is present in its sub-tree).
 *
 * Let's have following hierarchical tree of categories (primary keys are in brackets):
 *
 * - TV (1)
 * - Crt (2)
 * - LCD (3)
 * - big (4)
 * - small (5)
 * - Plasma (6)
 * - Fridges (7)
 *
 * When constraint `withinHierarchy('category', 1)` is used in a query targeting product entities only products that
 * relates directly to categories: `TV`, `Crt`, `LCD`, `big`, `small` and `Plasma` will be returned. Products in `Fridges`
 * will be omitted because they are not in a sub-tree of `TV` hierarchy.
 *
 * Only single `withinHierarchy` constraint can be used in the query.
 *
 * Example:
 *
 * ```
 * withinHierarchy('category', 4)
 * ```
 *
 * If you want to constraint the entity that you're querying on you can also omit entity type specification. See example:
 *
 * ```
 * query(
 * entities('CATEGORY'),
 * filterBy(
 * withinHierarchy(5)
 * )
 * )
 * ```
 *
 * This query will return all categories that belong to the sub-tree of category with primary key equal to 5.
 *
 * If you want to list all entities from the root level you need to use different constraint - `withinRootHierarchy` that
 * has the same notation but doesn't specify the id of the root level entity:
 *
 * ```
 * query(
 * entities('CATEGORY'),
 * filterBy(
 * withinRootHierarchy()
 * )
 * )
 * ```
 *
 * This query will return all categories within `CATEGORY` entity.
 *
 * You may use this constraint to list entities that refers to the hierarchical entities:
 *
 * ```
 * query(
 * entities('PRODUCT'),
 * filterBy(
 * withinRootHierarchy('CATEGORY')
 * )
 * )
 * ```
 *
 * This query returns all products that are attached to any category. Although, this query doesn't make much sense it starts
 * to be useful when combined with additional inner constraints described in following paragraphs.
 *
 * You can use additional sub constraints in `withinHierarchy` constraint: {@link DirectRelation}, {@link ExcludingRoot}
 * and {@link Excluding}
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class WithinHierarchy extends AbstractFilterConstraintContainer implements HierarchyFilterConstraint {
	private static final long serialVersionUID = 5346689836560255185L;

	private WithinHierarchy(Serializable[] argument, FilterConstraint[] fineGrainedConstraints) {
		super(argument, fineGrainedConstraints);
		checkInnerConstraintValidity(fineGrainedConstraints);
	}

	public WithinHierarchy(Integer ofParent, HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		super(ofParent, fineGrainedConstraints);
		checkInnerConstraintValidity(fineGrainedConstraints);
	}

	public WithinHierarchy(Serializable entityType, Integer ofParent, HierarchySpecificationFilterConstraint... fineGrainedConstraints) {
		super(entityType, ofParent, fineGrainedConstraints);
		checkInnerConstraintValidity(fineGrainedConstraints);
	}

	/**
	 * Returns name of the entity this hierarchy constraint relates to.
	 * Returns null if entity type is not specified and thus the same entity type as "queried" should be used.
	 */
	@Override
	@Nullable
	public <T extends Serializable & Comparable<?>> T getEntityType() {
		final Serializable firstArgument = getArguments()[0];
		//noinspection unchecked
		return firstArgument instanceof Integer ? null : (T) firstArgument;
	}

	/**
	 * Returns true if withinHierarchy should return only entities directly related to the {@link #getParentId()} entity.
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

	/**
	 * Returns id of the entity in which hierarchy to search.
	 */
	public int getParentId() {
		final Serializable[] arguments = getArguments();
		return arguments[0] instanceof Integer ? (Integer) arguments[0] : (Integer) arguments[1];
	}

	/**
	 * Returns true if withinHierarchy should not return entities directly related to the {@link #getParentId()} entity.
	 */
	public boolean isExcludingRoot() {
		return Arrays.stream(getConstraints())
			.anyMatch(ExcludingRoot.class::isInstance);
	}

	@Override
	public boolean isNecessary() {
		return super.isNecessary() || isApplicable();
	}

	@Override
	public boolean isApplicable() {
		final Serializable[] arguments = getArguments();
		return arguments.length >= 2 || (arguments.length >= 1 && arguments[0] instanceof Integer);
	}

	@Override
	public FilterConstraint getCopyWithNewChildren(FilterConstraint[] innerConstraints) {
		return new WithinHierarchy(getArguments(), innerConstraints);
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new WithinHierarchy(newArguments, getConstraints());
	}

	private void checkInnerConstraintValidity(FilterConstraint[] fineGrainedConstraints) {
		for (FilterConstraint filterConstraint : fineGrainedConstraints) {
			Assert.isTrue(
				filterConstraint instanceof DirectRelation ||
					filterConstraint instanceof Excluding ||
					filterConstraint instanceof ExcludingRoot,
				"Constraint withinHierarchy accepts only DirectRelation, ExcludingRoot and Excluding as inner constraints!"
			);
		}
	}
}
