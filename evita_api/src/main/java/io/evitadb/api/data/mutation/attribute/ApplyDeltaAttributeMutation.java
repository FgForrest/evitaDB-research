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

package io.evitadb.api.data.mutation.attribute;

import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.utils.Assert;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Increments or decrements existing numeric value by specified delta (negative number produces decrementation of
 * existing number, positive one incrementation).
 *
 * Allows to specify the number range that is tolerated for the value after delta application has been finished to
 * verify for example that number of items on stock doesn't go below zero.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ApplyDeltaAttributeMutation extends AttributeSchemaEvolvingMutation {
	private static final long serialVersionUID = -107926476337012921L;
	@Getter private final Number delta;
	@Getter private final NumberRange requiredRangeAfterApplication;

	public ApplyDeltaAttributeMutation(@Nonnull String attributeName, @Nonnull Number delta) {
		super(new AttributeKey(attributeName));
		this.delta = delta;
		this.requiredRangeAfterApplication = null;
	}

	public ApplyDeltaAttributeMutation(@Nonnull String attributeName, @Nullable Locale locale, @Nonnull Number delta) {
		super(new AttributeKey(attributeName, locale));
		this.delta = delta;
		this.requiredRangeAfterApplication = null;
	}

	public ApplyDeltaAttributeMutation(@Nonnull String attributeKey, @Nonnull Number delta, @Nullable NumberRange requiredRangeAfterApplication) {
		super(new AttributeKey(attributeKey));
		this.delta = delta;
		this.requiredRangeAfterApplication = requiredRangeAfterApplication;
	}

	public ApplyDeltaAttributeMutation(@Nonnull String attributeKey, @Nullable Locale locale, @Nonnull Number delta, @Nullable NumberRange requiredRangeAfterApplication) {
		super(new AttributeKey(attributeKey, locale));
		this.delta = delta;
		this.requiredRangeAfterApplication = requiredRangeAfterApplication;
	}

	@Override
	@Nonnull
	public Number getAttributeValue() {
		return delta;
	}

	@Nonnull
	@Override
	public AttributeValue mutateLocal(@Nullable AttributeValue existingAttributeValue) {
		Assert.isTrue(
			existingAttributeValue != null && existingAttributeValue.exists() && existingAttributeValue.getValue() != null,
			"Cannot apply delta to attribute " + attributeKey.getAttributeName() + " when it doesn't exist!"
		);
		Assert.isTrue(
				existingAttributeValue.getValue() instanceof Number,
				"Cannot apply delta to attribute " + attributeKey.getAttributeName() + " when its value is " +
						existingAttributeValue.getValue().getClass().getName()
		);
		final Number existingValue = (Number) existingAttributeValue.getValue();
		final Number newValue;
		if (existingValue instanceof BigDecimal) {
			newValue = ((BigDecimal) existingValue).add((BigDecimal) delta);
		} else if (existingValue instanceof Byte) {
			newValue = (byte) ((byte) existingValue + (byte) delta);
		} else if (existingValue instanceof Short) {
			newValue = (short) ((short)existingValue + (short)delta);
		} else if (existingValue instanceof Integer) {
			newValue = (int)existingValue + (int)delta;
		} else if (existingValue instanceof Long) {
			newValue = (long)existingValue + (long)delta;
		} else {
			// this should never ever happen
			throw new InvalidMutationException("Unknown Evita data type: " + existingValue.getClass());
		}
		if (requiredRangeAfterApplication != null) {
			Assert.isTrue(
				requiredRangeAfterApplication.isWithin(newValue),
				() -> new InvalidMutationException(
					"Applying delta " + delta + " on " + existingValue + " produced result " + newValue +
						" which is out of specified range " + requiredRangeAfterApplication + "!"
				)
			);
		}
		return new AttributeValue(existingAttributeValue.getVersion() + 1, attributeKey, newValue);
	}

	@Override
	public long getPriority() {
		return PRIORITY_UPSERT;
	}
}
