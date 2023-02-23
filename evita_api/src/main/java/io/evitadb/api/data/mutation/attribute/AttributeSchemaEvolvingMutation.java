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
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.function.UnaryOperator;

/**
 * Abstract parent for all attribute mutations that require schema validation / evolution.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class AttributeSchemaEvolvingMutation extends AttributeMutation implements SchemaEvolvingLocalMutation<AttributeValue, AttributeKey> {
	private static final long serialVersionUID = 2509373417337487380L;

	protected AttributeSchemaEvolvingMutation(@Nonnull AttributeKey attributeKey) {
		super(attributeKey);
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return attributeKey;
	}

	@Nonnull
	@Override
	public EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		return verifyOrEvolveSchema(
			schema,
			schema.getAttribute(attributeKey.getAttributeName()),
			getAttributeValue(),
			() -> {
				final EntitySchemaBuilder schemaBuilder = schema.open(schemaUpdater);
				if (attributeKey.isLocalized()) {
					schemaBuilder.withLocale(attributeKey.getLocale());
				}
				return schemaBuilder
					.withAttribute(
						attributeKey.getAttributeName(),
						getAttributeValue().getClass(),
						whichIs -> whichIs.localized(attributeKey::isLocalized)
					)
					.applyChanges();
			}
		);
	}

	public abstract Serializable getAttributeValue();

}
