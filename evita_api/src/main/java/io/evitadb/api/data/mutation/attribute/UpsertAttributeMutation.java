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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Upsert attribute mutation will either update existing attribute or create new one.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class UpsertAttributeMutation extends AttributeSchemaEvolvingMutation {
	private static final long serialVersionUID = 4274174996930002364L;
	@Nonnull private final Serializable value;

	public UpsertAttributeMutation(@Nonnull AttributeKey attributeKey, @Nonnull Serializable value) {
		super(attributeKey);
		this.value = value;
	}

	@Override
	@Nonnull
	public Serializable getAttributeValue() {
		return value;
	}

	@Nonnull
	@Override
	public AttributeValue mutateLocal(@Nullable AttributeValue existingValue) {
		if (existingValue == null) {
			// create new attribute value
			return new AttributeValue(attributeKey, value);
		} else {
			// update attribute version (we changed it) and return mutated value
			return new AttributeValue(existingValue.getVersion() + 1, attributeKey, this.value);
		}
	}

	@Override
	public long getPriority() {
		return PRIORITY_UPSERT;
	}
}
