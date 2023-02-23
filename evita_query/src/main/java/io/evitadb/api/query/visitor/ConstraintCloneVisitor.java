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

package io.evitadb.api.query.visitor;

import io.evitadb.api.query.Constraint;
import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import static java.util.Optional.ofNullable;

/**
 * Returns this query or copy of this query applying the transformation logic.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ConstraintCloneVisitor<T extends Constraint<T>> implements ConstraintVisitor<T> {
	private final Deque<List<T>> levelConstraints = new LinkedList<>();
	private final BiFunction<ConstraintCloneVisitor<T>, T, T> constraintTranslator;
	private T result = null;

	private ConstraintCloneVisitor() {
		this(null);
	}

	private ConstraintCloneVisitor(@Nullable BiFunction<ConstraintCloneVisitor<T>, T, T> constraintTranslator) {
		this.constraintTranslator = ofNullable(constraintTranslator).orElse((me, constraint) -> constraint);
	}

	public static <T extends Constraint<T>> T clone(@Nonnull T constraint, @Nullable BiFunction<ConstraintCloneVisitor<T>, T, T> constraintTranslator) {
		final ConstraintCloneVisitor<T> visitor = new ConstraintCloneVisitor<>(constraintTranslator);
		constraint.accept(visitor);
		return visitor.getResult();
	}

	@Override
	public void visit(@Nonnull T constraint) {
		if (constraint instanceof ConstraintContainer<?>) {
			//noinspection unchecked
			final ConstraintContainer<T> container = (ConstraintContainer<T>) constraint;
			final T translatedConstraint = constraintTranslator.apply(this, constraint);
			if (translatedConstraint == constraint) {
				levelConstraints.push(new ArrayList<>(container.getConstraintCount()));
				for (T innerConstraint : container) {
					innerConstraint.accept(this);
				}
				final List<T> innerConstraints = levelConstraints.pop();
				if (isEqual(container.getConstraints(), innerConstraints)) {
					addOnCurrentLevel(constraint);
				} else {
					createNewContainerWithReducedChildren(constraint, container, innerConstraints);
				}
			} else {
				addOnCurrentLevel(translatedConstraint);
			}
		} else if (constraint instanceof ConstraintLeaf) {
			addOnCurrentLevel(constraintTranslator.apply(this, constraint));
		}
	}

	/**
	 * Method traverses the passed container applying cloning logic of this visitor. The method is expected to be called
	 * from within the {@link #constraintTranslator} lambda.
	 */
	public List<T> analyseChildren(ConstraintContainer<T> constraint) {
		levelConstraints.push(new ArrayList<>(constraint.getConstraintCount()));
		for (T innerConstraint : constraint) {
			innerConstraint.accept(this);
		}
		return levelConstraints.pop();
	}

	public T getResult() {
		return result;
	}

	/**
	 * Creates new immutable container with reduced count of children.
	 */
	private void createNewContainerWithReducedChildren(T constraint, ConstraintContainer<T> container, List<T> reducedChildren) {
		//noinspection unchecked
		final T[] newChildren = reducedChildren.toArray(size -> (T[]) Array.newInstance(constraint.getType(), size));
		final T copyWithNewChildren = container.getCopyWithNewChildren(newChildren);
		if (copyWithNewChildren.isApplicable()) {
			addOnCurrentLevel(getFlattenedResult(copyWithNewChildren));
		}
	}

	/**
	 * Adds normalized constraint to the new composition.
	 */
	private void addOnCurrentLevel(T constraint) {
		if (constraint != null && constraint.isApplicable()) {
			if (levelConstraints.isEmpty()) {
				result = getFlattenedResult(constraint);
			} else {
				levelConstraints.peek().add(getFlattenedResult(constraint));
			}
		}
	}

	/**
	 * Flattens constraint container if it's not necessary according to {@link ConstraintContainer#isNecessary()} logic.
	 */
	private T getFlattenedResult(T constraint) {
		if (constraint instanceof ConstraintContainer) {
			//noinspection unchecked
			final ConstraintContainer<T> constraintContainer = (ConstraintContainer<T>) constraint;
			if (constraintContainer.isNecessary()) {
				return constraint;
			} else {
				final T[] innerConstraints = constraintContainer.getConstraints();
				if (innerConstraints.length == 1) {
					return innerConstraints[0];
				} else {
					throw new IllegalStateException(
						"Constraint container " + constraintContainer.getName() + " states it's not necessary, " +
							"but holds not exactly one children (" + innerConstraints.length + ")!"
					);
				}
			}
		} else {
			return constraint;
		}
	}

	/**
	 * Returns true only if array and list contents are same - i.e. have same quantity, and same instances (in terms of
	 * reference identity).
	 */
	private boolean isEqual(T[] constraints, List<T> comparedConstraints) {
		if (constraints.length != comparedConstraints.size()) {
			return false;
		}
		for (int i = 0; i < constraints.length; i++) {
			T constraint = constraints[i];
			T comparedConstraint = comparedConstraints.get(i);
			if (constraint != comparedConstraint) {
				return false;
			}
		}
		return true;
	}
}
