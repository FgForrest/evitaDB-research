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

import io.evitadb.api.query.FilterConstraint;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * This `inRange` is constraint that compares value of the attribute with name passed in first argument with the date
 * and time passed in the second argument. First argument must be {@link String}, second argument must be {@link ZonedDateTime}
 * type. If second argument is not passed - current date and time (now) is used.
 * Type of the attribute value must implement [Range](classes/range_interface.md) interface.
 *
 * Function returns true if second argument is greater than or equal to range start (from), and is lesser than
 * or equal to range end (to).
 *
 * Example:
 *
 * ```
 * inRange('valid', 2020-07-30T20:37:50+00:00)
 * inRange('age', 18)
 * ```
 *
 * Function supports attribute arrays and when attribute is of array type `inRange` returns true if *any of attribute* values
 * has range, that envelopes the passed value the value in the constraint. If we have the attribute `age` with value
 * `[[18, 25],[60,65]]` all these constraints will match:
 *
 * ```
 * inRange('age', 18)
 * inRange('age', 24)
 * inRange('age', 63)
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InRange extends AbstractAttributeFilterConstraintLeaf implements IndexUsingConstraint {
	private static final long serialVersionUID = -6018832750772234247L;

	private InRange(Serializable... arguments) {
		super(arguments);
	}

	public InRange(String attributeName) {
		super(attributeName);
	}

	public InRange(String attributeName, ZonedDateTime theMoment) {
		super(attributeName, theMoment);
	}

	public InRange(String attributeName, Number theValue) {
		super(attributeName, theValue);
	}

	/**
	 * Returns {@link Serializable} argument that should be verified whether is within the range (inclusive) of attribute validity.
	 */
	@Nullable
	public Serializable getUnknownArgument() {
		final boolean argsReady = getArguments().length == 2;
		if (argsReady) {
			return getArguments()[1];
		} else {
			return null;
		}
	}

	/**
	 * Returns {@link ZonedDateTime} that should be verified whether is within the range (inclusive) of attribute validity.
	 */
	@Nullable
	public ZonedDateTime getTheMoment() {
		final boolean argsReady = getArguments().length == 2;
		if (argsReady) {
			return getArguments()[1] instanceof ZonedDateTime ? (ZonedDateTime) getArguments()[1] : null;
		} else {
			return null;
		}
	}

	/**
	 * Returns {@link Number} that should be verified whether is within the range (inclusive) of attribute validity.
	 */
	@Nullable
	public Number getTheValue() {
		final boolean argsReady = getArguments().length == 2;
		if (argsReady) {
			return getArguments()[1] instanceof Number ? (Number) getArguments()[1] : null;
		} else {
			return null;
		}
	}

	@Override
	public boolean isApplicable() {
		return getArguments().length > 0;
	}

	@Override
	public FilterConstraint cloneWithArguments(Serializable[] newArguments) {
		return new InRange(newArguments);
	}
}
