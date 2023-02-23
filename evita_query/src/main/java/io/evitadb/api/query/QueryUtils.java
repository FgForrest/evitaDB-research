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

import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.visitor.FinderVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Class contains various utility method for accessing and manipulating the {@link Query}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class QueryUtils {

	private QueryUtils() {
	}

	/**
	 * Method finds filtering constraint of specified type in the passed query and returns it.
	 */
	@Nullable
	public static <T extends FilterConstraint> T findFilter(@Nonnull Query query, @Nonnull Class<T> filterType) {
		return Optional.ofNullable(query.getFilterBy()).map(it -> findFilter(it, filterType)).orElse(null);
	}

	/**
	 * Method finds filtering constraint of specified type in the passed query and returns it.
	 */
	@Nullable
	public static <T extends FilterConstraint> T findFilter(@Nonnull FilterConstraint filterBy, @Nonnull Class<T> filterType) {
		//noinspection unchecked
		return (T) FinderVisitor.findConstraint(filterBy, filterType::isInstance);
	}

	/**
	 * Method finds filtering constraint by passed predicate.
	 */
	@Nullable
	public static <T extends FilterConstraint> T findFilter(@Nonnull FilterConstraint filterBy, @Nonnull Predicate<FilterConstraint> predicate) {
		//noinspection unchecked
		return (T) FinderVisitor.findConstraint(filterBy, predicate);
	}

	/**
	 * Method finds all filtering constraints of specified type in the passed query and returns them.
	 */
	@Nonnull
	public static <T extends FilterConstraint> List<T> findFilters(@Nonnull Query query, @Nonnull Class<T> filterType) {
		return Optional.ofNullable(query.getFilterBy()).map(it -> findFilters(it, filterType)).orElse(Collections.emptyList());
	}

	/**
	 * Method finds all filtering constraints of specified type in the passed query and returns them.
	 */
	@Nonnull
	public static <T extends FilterConstraint> List<T> findFilters(FilterBy filterBy, @Nonnull Class<T> filterType) {
		//noinspection unchecked
		return (List<T>) FinderVisitor.findConstraints(filterBy, filterType::isInstance);
	}

	/**
	 * Method finds ordering constraint of specified type in the passed query and returns it.
	 */
	@Nullable
	public static <T extends OrderConstraint> T findOrder(@Nonnull Query query, @Nonnull Class<T> orderType) {
		//noinspection unchecked
		return Optional.ofNullable(query.getOrderBy())
			.map(it -> (T) FinderVisitor.findConstraint(it, orderType::isInstance))
			.orElse(null);
	}

	/**
	 * Method finds requirement constraint of specified type in the passed query and returns it.
	 */
	@Nullable
	public static <T extends RequireConstraint> T findRequire(@Nonnull Query query, @Nonnull Class<T> requireType) {
		return Optional.ofNullable(query.getRequire()).map(it -> findRequire(it, requireType)).orElse(null);
	}

	/**
	 * Method finds requirement constraint of specified type in the passed query and returns it.
	 */
	@Nullable
	public static <T extends RequireConstraint> T findRequire(@Nonnull RequireConstraint container, @Nonnull Class<T> requireType) {
		//noinspection unchecked
		return (T) FinderVisitor.findConstraint(container, requireType::isInstance);
	}

	/**
	 * Method finds requirement constraint of specified type in the passed query and returns it. Lookup will ignore
	 * all containers that are assignable to `stopContainerType`.
	 */
	@Nullable
	public static <T extends RequireConstraint> T findRequire(@Nonnull Query query, @Nonnull Class<T> requireType, @Nonnull Class<? extends RequireConstraint> stopContainerType) {
		//noinspection unchecked
		return Optional.ofNullable(query.getRequire())
			.map(it -> (T) FinderVisitor.findConstraint(it, requireType::isInstance, stopContainerType::isInstance))
			.orElse(null);
	}

	/**
	 * Method finds all requirement constraints of specified type in the passed query and returns them.
	 */
	@Nonnull
	public static <T extends RequireConstraint> List<T> findRequires(@Nonnull Query query, @Nonnull Class<T> requireType) {
		//noinspection unchecked
		return Optional.ofNullable(query.getRequire())
			.map(it -> (List<T>) FinderVisitor.findConstraints(it, requireType::isInstance))
			.orElse(Collections.emptyList());
	}

	/**
	 * Method returns true if passed two values are not equals. When first value is comparable, method compareTo is used
	 * instead of equals (to correctly match {@link java.math.BigDecimal}).
	 */
	public static boolean valueDiffers(@Nullable Serializable thisValue, @Nullable Serializable otherValue) {
		if (thisValue instanceof Object[]) {
			if (!(otherValue instanceof Object[])) {
				return true;
			}
			final Object[] thisValueArray = (Object[]) thisValue;
			final Object[] otherValueArray = (Object[]) otherValue;
			if (thisValueArray.length != otherValueArray.length) {
				return true;
			}
			for (int i = 0; i < thisValueArray.length; i++) {
				if (valueDiffersInternal((Serializable) thisValueArray[i], (Serializable) otherValueArray[i])) {
					return true;
				}
			}
			return false;
		} else {
			return valueDiffersInternal(thisValue, otherValue);
		}
	}

	private static boolean valueDiffersInternal(@Nullable Serializable thisValue, @Nullable Serializable otherValue) {
		// when value is Comparable (such as BigDecimal!) - use compareTo function instead of equals
		if (thisValue instanceof Comparable) {
			if (otherValue == null) {
				return true;
			} else {
				return !thisValue.getClass().isInstance(otherValue) || ((Comparable) thisValue).compareTo(otherValue) != 0;
			}
		} else {
			return !Objects.equals(thisValue, otherValue);
		}
	}
}
