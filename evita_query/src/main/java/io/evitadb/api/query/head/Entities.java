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

package io.evitadb.api.query.head;

import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.HeadConstraint;

import java.io.Serializable;

/**
 * Each query must specify collection. This mandatory {@link Serializable} constraint controls what collection
 * the query will be applied on.
 *
 * Sample of the header is:
 *
 * ```
 * collection('category')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Entities extends ConstraintLeaf<HeadConstraint> implements HeadConstraint {
	private static final long serialVersionUID = -7064678623633579615L;

	private Entities(Serializable... arguments) {
		super(arguments);
	}

	public Entities(Serializable entityType) {
		super(entityType);
	}

	/**
	 * Returns type of the entity that will be queried by associated query.
	 * @return
	 */
	public <T extends Serializable & Comparable<?>> T getEntityType() {
		//noinspection unchecked
		return (T) getArguments()[0];
	}

	@Override
	public Class<HeadConstraint> getType() {
		return HeadConstraint.class;
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 1;
	}

	@Override
	public void accept(ConstraintVisitor<HeadConstraint> visitor) {
		visitor.visit(this);
	}

	@Override
	public HeadConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Entities(newArguments);
	}
}
