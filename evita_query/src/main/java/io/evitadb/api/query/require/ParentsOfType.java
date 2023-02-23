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

import java.io.Serializable;
import java.util.Arrays;

/**
 * This `parentsOfType` require constraint can be used only
 * for [hierarchical entities](../model/entity_model.md#hierarchical-placement) and can have zero, one or more
 * [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html) arguments that specifies type of
 * hierarchical entity that this entity relates to. If argument is omitted, entity type of queried entity is used.
 * Constraint may have also inner require constraints that define how rich returned information should be (by default only
 * primary keys are returned, but full entities might be returned as well).
 *
 * When this require constraint is used an additional object is stored to result index. This data structure contains
 * information about referenced entity paths for each entity in the response.
 *
 * Example for returning parents of the category when entity type `product` is queried:
 *
 * ```
 * parentsOfType('category')
 * parentsOfType('category','brand')
 * ```
 *
 * Additional data structure by default returns only primary keys of those entities, but it can also provide full parent
 * entities when this form of constraint is used:
 *
 * ```
 * parentsOfType('category', entityBody())
 * parentsOfType('category', 'brand', entityBody())
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ParentsOfType extends AbstractRequireConstraintContainer implements SeparateEntityContentRequireContainer, ExtraResultRequireConstraint {
	private static final long serialVersionUID = -8462717866711769929L;

	public ParentsOfType(Serializable... entityType) {
		super(entityType);
	}

	public ParentsOfType(Serializable[] entityType, EntityContentRequire[] requirements) {
		super(entityType, requirements);
	}

	private ParentsOfType(Serializable[] entityType, RequireConstraint[] requirements) {
		super(entityType, requirements);
	}

	/**
	 * Returns entity types which parent structure should be loaded for selected entity.
	 */
	public Serializable[] getEntityTypes() {
		return getArguments();
	}

	/**
	 * Returns requirement constraints for the loaded entitye.
	 */
	public EntityContentRequire[] getRequirements() {
		return Arrays.stream(getConstraints()).map(EntityContentRequire.class::cast).toArray(EntityContentRequire[]::new);
	}

	@Override
	public boolean isNecessary() {
		return getArguments().length > 0;
	}

	@Override
	public boolean isApplicable() {
		return true;
	}

	@Override
	public RequireConstraint getCopyWithNewChildren(RequireConstraint[] innerConstraints) {
		return new ParentsOfType(getEntityTypes(), innerConstraints);
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new ParentsOfType(newArguments, getConstraints());
	}
}
