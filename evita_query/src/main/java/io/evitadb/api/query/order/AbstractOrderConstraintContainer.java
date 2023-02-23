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

package io.evitadb.api.query.order;

import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.OrderConstraint;

import java.io.Serializable;

/**
 * Represents base constraint container accepting only ordering constraints.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
abstract class AbstractOrderConstraintContainer extends ConstraintContainer<OrderConstraint> implements OrderConstraint {
	private static final long serialVersionUID = -7858636742421451053L;

	protected AbstractOrderConstraintContainer(Serializable[] arguments, OrderConstraint... children) {
		super(arguments, children);
	}

	protected AbstractOrderConstraintContainer(Serializable argument, OrderConstraint... children) {
		super(new Serializable[] {argument}, children);
	}

	protected AbstractOrderConstraintContainer(OrderConstraint... children) {
		super(children);
	}

	@Override
	public Class<OrderConstraint> getType() {
		return OrderConstraint.class;
	}

	@Override
	public void accept(ConstraintVisitor<OrderConstraint> visitor) {
		visitor.visit(this);
	}

}