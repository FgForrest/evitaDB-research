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
import java.util.Arrays;

/**
 * This `primaryKey` is constraint that accepts set of {@link Integer}
 * that represents primary keys of the entities that should be returned.
 *
 * Function returns true if entity primary key is part of the passed set of integers.
 * This form of entity lookup function is the fastest one.
 *
 * Only single `primaryKey` constraint can be used in the query.
 *
 * Example:
 *
 * ```
 * primaryKey(1, 2, 3)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PrimaryKey extends AbstractFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = -6950287451642746676L;

	private PrimaryKey(Serializable... arguments) {
		super(arguments);
	}

	public PrimaryKey(Integer... primaryKey) {
		super(primaryKey);
	}

	/**
	 * Returns primary keys of entities to lookup for.
	 */
	@Nonnull
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
		return new PrimaryKey(newArguments);
	}
}
