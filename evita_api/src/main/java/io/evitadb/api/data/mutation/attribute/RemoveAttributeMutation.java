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
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Remove attribute mutation will drop existing attribute - ie.generates new version of the attribute with tombstone
 * on it.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class RemoveAttributeMutation extends AttributeMutation {
	private static final long serialVersionUID = 7072678664245663785L;

	public RemoveAttributeMutation(@Nonnull AttributeKey attributeKey) {
		super(attributeKey);
	}

	@Nonnull
	@Override
	public AttributeValue mutateLocal(@Nullable AttributeValue existingValue) {
		Assert.isTrue(
			existingValue != null && existingValue.exists(),
			() -> new InvalidMutationException(
				"Cannot remove " + attributeKey.getAttributeName() + " attribute - it doesn't exist!"
			)
		);
		return new AttributeValue(existingValue.getVersion() + 1, existingValue.getKey(), existingValue.getValue(), true);
	}

	@Override
	public long getPriority() {
		return PRIORITY_REMOVAL;
	}
}
