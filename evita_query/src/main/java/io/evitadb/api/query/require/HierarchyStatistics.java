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

package io.evitadb.api.query.require;

import io.evitadb.api.query.RequireConstraint;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;

/**
 * This `hierarchyStatistics` require constraint triggers computing the statistics for referenced hierarchical entities
 * and adds an object to the result index. It has at least one {@link Serializable}
 * argument that specifies type of hierarchical entity that this entity relates to. Additional arguments allow passing
 * requirements for fetching the referenced entity contents so that there are no other requests to the Evita DB necessary
 * and all data are fetched in single query.
 *
 * When this require constraint is used an additional object is stored to result index:
 *
 * - **HierarchyStatistics**
 * this object is organized in the tree structure that reflects the hierarchy of the entities of desired type that are
 * referenced by entities returned by primary query, for each tree entity there is a number that represents the count of
 * currently queried entities that relates to that referenced hierarchical entity and match the query filter - either
 * directly or to some subordinate entity of this hierarchical entity
 *
 * Example:
 *
 * <pre>
 * hierarchyStatistics('category')
 * hierarchyStatistics('category', entityBody(), attributes())
 * </pre>
 *
 * This require constraint is usually used when hierarchical menu rendering is needed. For example when we need to render
 * menu for entire e-commerce site, but we want to take excluded subtrees into an account and also reflect the filtering
 * conditions that may filter out dozens of products (and thus leading to empty categories) we can invoke following query:
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
 * This query would return first page with 20 products (omitting hundreds of others on additional pages) but also returns a
 * HierarchyStatistics in additional data. This object may contain following structure:
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
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class HierarchyStatistics extends AbstractRequireConstraintContainer implements SeparateEntityContentRequireContainer, ExtraResultRequireConstraint {
	private static final long serialVersionUID = -9099245444284958650L;

	private HierarchyStatistics(Serializable[] arguments, RequireConstraint... children) {
		super(arguments, children);
	}

	public HierarchyStatistics(Serializable entityType, EntityContentRequire... requirements) {
		super(new Serializable[]{entityType}, requirements);
	}

	/**
	 * Returns name of the entity this hierarchy constraint relates to.
	 */
	@Nullable
	public Serializable getEntityType() {
		return getArguments().length == 0 ? null : getArguments()[0];
	}

	/**
	 * Returns requirement constraints for the loaded entitye.
	 */
	public EntityContentRequire[] getRequirements() {
		return Arrays.stream(getConstraints()).map(EntityContentRequire.class::cast).toArray(EntityContentRequire[]::new);
	}

	@Override
	public boolean isNecessary() {
		return true;
	}

	@Override
	public boolean isApplicable() {
		return true;
	}

	@Override
	public RequireConstraint getCopyWithNewChildren(RequireConstraint[] innerConstraints) {
		return new HierarchyStatistics(
			getArguments(),
			innerConstraints
		);
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new HierarchyStatistics(
			newArguments,
			getConstraints()
		);
	}

}
