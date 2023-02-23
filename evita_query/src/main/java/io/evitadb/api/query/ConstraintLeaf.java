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

package io.evitadb.api.query;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Leaf constraint is final constraint of the constraint tree. It cannot contain any other constraints. On the contrary
 * it contains on arguments.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class ConstraintLeaf<T extends Constraint<T>> extends BaseConstraint<T> {
	private static final long serialVersionUID = 3842640572690004094L;

	protected ConstraintLeaf(Serializable... arguments) {
		super(arguments);
		if (Arrays.stream(arguments).anyMatch(Constraint.class::isInstance)) {
			throw new IllegalArgumentException("Constraint argument is not allowed for leaf constraint (" + getName() + ").");
		}
	}

}
