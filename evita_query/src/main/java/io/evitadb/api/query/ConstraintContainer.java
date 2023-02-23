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

import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Constraint is a constraint that allows nesting additional constraints. Actually there is no constraint container
 * that would allow to combine inner constraints with arguments, but we don't want to close the gate to this, so
 * therefore arguments are defined in the base class this one extends.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@EqualsAndHashCode(callSuper = true, of = "children")
public abstract class ConstraintContainer<T extends Constraint<T>> extends BaseConstraint<T> implements Iterable<T> {
	private static final long serialVersionUID = -446936362470832956L;
	private static final Serializable[] NO_ARGS = new Serializable[0];
	private final T[] children;

	@SafeVarargs
	protected ConstraintContainer(Serializable[] arguments, T... children) {
		super(arguments);
		if (children.length > 0 && !getType().isAssignableFrom(children.getClass().getComponentType())) {
			throw new IllegalArgumentException(
					children.getClass().getComponentType() + " is not of expected type " + getType()
			);
		}
		// filter out null values, but avoid creating new array if not necessary
		if (Arrays.stream(children).anyMatch(Objects::isNull)) {
			//noinspection unchecked
			this.children = Arrays.stream(children)
					.filter(Objects::nonNull)
					.toArray(size -> (T[]) Array.newInstance(getType(), size));
		} else {
			this.children = children;
		}
	}

	@SafeVarargs
	protected ConstraintContainer(T... children) {
		this(NO_ARGS, children);
	}

	/**
	 * Returns array of constraint children.
	 *
	 * @return
	 */
	public T[] getConstraints() {
		return children;
	}

	/**
	 * Returns count of constraint children.
	 *
	 * @return
	 */
	public int getConstraintCount() {
		return children.length;
	}

	/**
	 * Returns an iterator over a set of elements of type T.
	 *
	 * @return an Iterator.
	 */
	@Nonnull
	@Override
	public Iterator<T> iterator() {
		return Arrays
				.stream(children)
				.iterator();
	}

	/**
	 * Returns true if there is more than one constraint - if not this container is probably useless (in most cases).
	 * @return
	 */
	public boolean isNecessary() {
		return children.length > 1;
	}

	/**
	 * Returns true if constraint has enough data to be used in query.
	 * False in case constraint has no sense - because it couldn't be processed in current state (for
	 * example significant arguments are missing or are invalid).
	 *
	 * @return
	 */
	@Override
	public boolean isApplicable() {
		return children.length > 0;
	}

	/**
	 * Returns copy of this container type with new children.
	 *
	 * @param innerConstraints
	 * @return
	 */
	public abstract T getCopyWithNewChildren(T[] innerConstraints);

	@Override
	public String toString() {
		return getName() +
				ARG_OPENING +
				Stream.concat(
					Arrays.stream(getArguments()).map(BaseConstraint::convertToString),
					Arrays.stream(children).map(Constraint::toString)
				)
						.collect(Collectors.joining(",")) +
				ARG_CLOSING;
	}
}
