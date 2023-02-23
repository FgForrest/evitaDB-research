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

package io.evitadb.storage.query.translate;

import io.evitadb.api.query.Constraint;
import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Visitor that removes constraints which fulfill passed predicate from tree and returns new tree
 * without removed constraints.
 *
 * <b>Note: </b> root constraint is expected to be container.
 *
 * Currently, used for computing histograms.
 *
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class ConstraintRemovingVisitor<T extends Constraint<T>> implements ConstraintVisitor<T> {

    /**
     * All constraints containing this attribute name will be removed from constraint tree.
     */
    private final Predicate<T> forbiddenConstraintPredicate;

    private final Deque<List<T>> currentConstraintsStack = new LinkedList<>();
    private T modifiedTree = null;

    @Override
    public void visit(@Nonnull T constraint) {
        if (constraint instanceof ConstraintContainer<?>) {
            @SuppressWarnings("unchecked") final ConstraintContainer<T> container = (ConstraintContainer<T>) constraint;

            currentConstraintsStack.push(new LinkedList<>());
            for (T innerConstraint : container.getConstraints()) {
                innerConstraint.accept(this);
            }
            final List<T> filteredInnerConstraints = currentConstraintsStack.pop();

            final T filteredContainer = getFilteredContainer(constraint, container, filteredInnerConstraints);
            if (currentConstraintsStack.isEmpty()) {
                modifiedTree = filteredContainer;
            } else {
                currentConstraintsStack.peek().add(filteredContainer);
            }
        } else if ((constraint instanceof ConstraintLeaf<?>) && constraint.isApplicable()) {
            if (forbiddenConstraintPredicate.test(constraint)) {
                return;
            }
            currentConstraintsStack.peek().add(constraint);
        }
    }

    /**
     * Compares original with filtered container and returns new copy only if children were filtered.
     */
    private T getFilteredContainer(T constraint,
                                   @Nonnull ConstraintContainer<T> originalContainer,
                                   @Nonnull List<T> filteredInnerConstraints) {
        final T filteredContainer;
        if (isEqual(originalContainer.getConstraints(), filteredInnerConstraints)) {
            //noinspection unchecked
            filteredContainer = (T) originalContainer;
        } else {
            //noinspection unchecked
            filteredContainer = originalContainer.getCopyWithNewChildren(
                    filteredInnerConstraints.toArray(size -> (T[]) Array.newInstance(constraint.getType(), size))
            );
        }
        return filteredContainer;
    }

    /**
     * Returns true only if array and list contents are same - i.e. has same count and same instances (in terms of reference
     * identity).
     */
    private boolean isEqual(T[] originalConstraints, List<T> comparedConstraints) {
        if (originalConstraints.length != comparedConstraints.size()) {
            return false;
        }
        for (int i = 0; i < originalConstraints.length; i++) {
            final T originalConstraint = originalConstraints[i];
            final T comparedConstraint = comparedConstraints.get(i);
            if (originalConstraint != comparedConstraint) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns final modified tree after original tree has been visited.
     */
    public T getModifiedTree() {
        if (modifiedTree == null) {
            throw new IllegalStateException("No constraint tree has been visited yet or visitor is not finished yet.");
        }
        return modifiedTree;
    }
}
