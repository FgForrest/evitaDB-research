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

import java.io.Serializable;
import java.util.Arrays;

/**
 * If you use {@link ExcludingRoot} sub-constraint in {@link WithinHierarchy} parent, you can specify one or more
 * Integer primary keys of the underlying entities which hierarchical subtree should be excluded from examination.
 *
 * Exclusion arguments allows excluding certain parts of the hierarchy tree from examination. This feature is used in
 * environments where certain sub-trees can be made "invisible" and should not be accessible to users, although they are
 * still part of the database.
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
 * When constraint `withinHierarchy('category', 1, excluding(3))` is used in a query targeting product entities,
 * only products that relate directly to categories: `TV`, `Crt` and `Plasma` will be returned. Products in `Fridges` will
 * be omitted because they are not in a sub-tree of `TV` hierarchy and products in `LCD` sub-tree will be omitted because
 * they're part of the excluded sub-trees.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Excluding extends AbstractFilterConstraintLeaf implements HierarchySpecificationFilterConstraint {
	private static final long serialVersionUID = -6950287451642746676L;

	private Excluding(Serializable... arguments) {
		super(arguments);
	}

	public Excluding(Integer... primaryKey) {
		super(primaryKey);
	}

	/**
	 * Returns primary keys of entities which trees should be excluded from {@link WithinHierarchy} constraint.
	 * @return
	 */
	public int[] getPrimaryKeys() {
		return Arrays.stream(getArguments())
			.mapToInt(Integer.class::cast)
			.toArray();
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length > 0;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Excluding(newArguments);
	}
}
