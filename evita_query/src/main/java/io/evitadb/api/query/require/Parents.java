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
 * This `parents` require constraint can be used only
 * for [hierarchical entities](../model/entity_model.md#hierarchical-placement) and target the entity type that is
 * requested in the query. Constraint may have also inner require constraints that define how rich returned information
 * should be (by default only primary keys are returned, but full entities might be returned as well).
 *
 * When this require constraint is used an additional object is stored to result index. This data structure contains
 * information about referenced entity paths for each entity in the response.
 *
 * Example for returning parents of the same type as was queried (e.g. parent categories of filtered category):
 *
 * ```
 * parents()
 * ```
 *
 * Additional data structure by default returns only primary keys of those entities, but it can also provide full parent
 * entities when this form of constraint is used:
 *
 * ```
 * parents()
 * parents(entityBody())
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Parents extends AbstractRequireConstraintContainer implements SeparateEntityContentRequireContainer, ExtraResultRequireConstraint {
	private static final long serialVersionUID = -4386649995372804764L;

	public Parents() {
		super();
	}

	public Parents(EntityContentRequire... requirements) {
		super(requirements);
	}

	private Parents(RequireConstraint[] children) {
		super(children);
	}

	/**
	 * Returns requirement constraints for the loaded entity.
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
		return new Parents(innerConstraints);
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		throw new UnsupportedOperationException("This type of constraint doesn't support arguments!");
	}
}
