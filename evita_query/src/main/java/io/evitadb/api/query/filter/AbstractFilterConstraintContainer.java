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

import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.FilterConstraint;

import java.io.Serializable;

/**
 * Represents base constraint container accepting only filtering constraints.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
abstract class AbstractFilterConstraintContainer extends ConstraintContainer<FilterConstraint> implements FilterConstraint {
	private static final long serialVersionUID = 1585533135394728582L;

	protected AbstractFilterConstraintContainer(Serializable[] arguments, FilterConstraint... children) {
		super(arguments, children);
	}

	protected AbstractFilterConstraintContainer(Serializable argument, FilterConstraint... children) {
		super(new Serializable[] {argument}, children);
	}

	protected AbstractFilterConstraintContainer(Serializable argument1, Serializable argument2, FilterConstraint... children) {
		super(new Serializable[] {argument1, argument2}, children);
	}

	protected AbstractFilterConstraintContainer(FilterConstraint... children) {
		super(children);
	}

	@Override
	public Class<FilterConstraint> getType() {
		return FilterConstraint.class;
	}

	@Override
	public void accept(ConstraintVisitor<FilterConstraint> visitor) {
		visitor.visit(this);
	}

}