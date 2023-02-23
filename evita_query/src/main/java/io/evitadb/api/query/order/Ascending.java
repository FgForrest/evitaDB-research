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

import io.evitadb.api.query.OrderConstraint;

import java.io.Serializable;

/**
 * This `ascending` is ordering that sorts returned entities by values in attribute with name passed in the first argument
 * in ascending order. Argument must be of {@link String} type.
 * Ordering is executed by natural order of the {@link Comparable}
 * type.
 *
 * Example:
 *
 * ```
 * ascending('age')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Ascending extends AbstractOrderAttributeConstraintLeaf {
	private static final long serialVersionUID = 7476427159537900535L;

	private Ascending(Serializable... arguments) {
		super(arguments);
	}

	public Ascending(String attributeName) {
		super(attributeName);
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length == 1;
	}

	@Override
	public OrderConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Ascending(newArguments);
	}
}
