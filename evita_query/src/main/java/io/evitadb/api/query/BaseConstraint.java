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

import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.utils.StringUtils;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base constraint defines shared behaviour for all constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode(of = {"name", "arguments"})
abstract class BaseConstraint<T extends Constraint<T>> implements Constraint<T> {
	private static final long serialVersionUID = 2216675116416057520L;
	private final String name = StringUtils.uncapitalize(this.getClass().getSimpleName());
	private final Serializable[] arguments;

	protected BaseConstraint(Serializable... arguments) {
		// filter out null values, but avoid creating new array if not necessary
		if (Arrays.stream(arguments).anyMatch(it -> it == null || it != EvitaDataTypes.toSupportedType(it))) {
			this.arguments = Arrays.stream(arguments)
					.filter(Objects::nonNull)
					.map(EvitaDataTypes::toSupportedType)
					.toArray(Serializable[]::new);
		} else {
			this.arguments = arguments;
		}
	}

	/**
	 * Name is always derived from the constraint class name to simplify searching/mapping query language to respective
	 * classes.
	 * @return
	 */
	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns arguments of the constraint.
	 * @return
	 */
	@Nonnull
	@Override
	public Serializable[] getArguments() {
		return arguments;
	}

	/**
	 * Rendering is shared among all constraints - it consists of `constraintName`(`arg1`,`arg2`,`argN`), argument can
	 * be constraint itself.
	 * @return
	 */
	@Override
	public String toString() {
		return getName() +
				ARG_OPENING +
				Arrays.stream(arguments)
						.map(BaseConstraint::convertToString)
						.collect(Collectors.joining(",")) +
				ARG_CLOSING;
	}

	static String convertToString(Serializable value) {
		return EvitaDataTypes.formatValue(value);
	}

}
