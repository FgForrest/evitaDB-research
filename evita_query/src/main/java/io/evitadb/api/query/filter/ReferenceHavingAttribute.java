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

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * This `referenceAttribute` container is filtering constraint that filters returned entities by their reference
 * attributes that must match the inner condition.
 *
 * Example:
 *
 * ```
 * referenceHavingAttribute(
 *    'CATEGORY',
 *    eq('code', 'KITCHENWARE')
 * )
 * ```
 *
 * or
 *
 * ```
 * referenceHavingAttribute(
 *    'CATEGORY',
 *    and(
 *       isTrue('visible'),
 *       eq('code', 'KITCHENWARE')
 *    )
 * )
 * ```
 *
 * TOBEDONE JNO - consider renaming to `referenceMatching`
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ReferenceHavingAttribute extends AbstractFilterConstraintContainer {
	private static final long serialVersionUID = -2727265686254207631L;

	private ReferenceHavingAttribute(Serializable[] arguments, FilterConstraint... children) {
		super(arguments, children);
	}

	public ReferenceHavingAttribute(Serializable entityType, FilterConstraint children) {
		super(entityType, children);
	}

	/**
	 * Returns entity type of the facet that should be used for applying for filtering according to children constraints.
	 * @return
	 */
	@Nonnull
	public Serializable getEntityType() {
		return getArguments()[0];
	}

	/**
	 * Returns constraint connected with this reference constraint (it must be exactly one).
	 * @return
	 */
	@Nonnull
	public FilterConstraint getConstraint() {
		return getConstraints()[0];
	}

	@Override
	public boolean isNecessary() {
		return getArguments().length == 1 && getConstraints().length == 1;
	}

	@Override
	public FilterConstraint getCopyWithNewChildren(FilterConstraint[] innerConstraints) {
		return new ReferenceHavingAttribute(getEntityType(), innerConstraints.length == 0 ? null : innerConstraints[0]);
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new ReferenceHavingAttribute(newArguments, getConstraints());
	}
}
