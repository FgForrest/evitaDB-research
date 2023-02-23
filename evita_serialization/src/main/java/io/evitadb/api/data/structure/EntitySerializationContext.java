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

package io.evitadb.api.data.structure;

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;

import java.util.function.Supplier;

/**
 * This context is used to pass actual {@link EntitySchema} reference to Kryo deserializer on deep levels
 * of the deserialization chain. Unfortunately the schema needs to be referenced on multiple places in the entity
 * to avoid circular references and also to allow schema validation as early as possible.
 *
 * Use method {@link #executeWithSupplier(EntitySchema, Runnable)} to pass current entity schema to the block of code
 * that deserialized sth entity contents from the binary stream.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EntitySerializationContext {
	private static final ThreadLocal<EntitySchema> ENTITY_SCHEMA_SUPPLIER = new ThreadLocal<>();

	private EntitySerializationContext() {
	}

	/**
	 * This method allows setting context-sensitive entity schema to the deserialization process. This is necessary
	 * to correctly and consistently schema during deserialization.
	 */
	public static <T> T executeWithSupplier(EntitySchema entitySchema, Supplier<T> lambda) {
		final EntitySchema existingSchemaSet = ENTITY_SCHEMA_SUPPLIER.get();
		try {
			Assert.isTrue(
				existingSchemaSet == null || existingSchemaSet.isCompatibleAndSameOrOlder(entitySchema),
				() -> new IllegalStateException("Entity schema has been already set (" + existingSchemaSet.getName() + " vs. " + entitySchema.getName() + ")! This should never happen!")
			);
			ENTITY_SCHEMA_SUPPLIER.set(entitySchema);
			return lambda.get();
		} finally {
			if (existingSchemaSet == null) {
				ENTITY_SCHEMA_SUPPLIER.remove();
			}
		}
	}

	/**
	 * This method allows to set context sensitive entity schema to the deserialization process. This is necessary
	 * to correctly and consistently schema during deserialization.
	 */
	public static void executeWithSupplier(EntitySchema entitySchema, Runnable lambda) {
		final EntitySchema existingSchemaSet = ENTITY_SCHEMA_SUPPLIER.get();
		try {
			Assert.isTrue(
				existingSchemaSet == null || existingSchemaSet.isCompatibleAndSameOrOlder(entitySchema),
				() -> new IllegalStateException("Entity schema has been already set (" + existingSchemaSet.getName() + " vs. " + entitySchema.getName() + ")! This should never happen!")
			);
			ENTITY_SCHEMA_SUPPLIER.set(entitySchema);
			lambda.run();
		} finally {
			ENTITY_SCHEMA_SUPPLIER.remove();
		}
	}

	/**
	 * Returns currently initialized {@link EntitySchema} for this entity. Returns non null value only when called
	 * inside {@link #executeWithSupplier(EntitySchema, Supplier)} context.
	 */
	public static EntitySchema getEntitySchema() {
		return ENTITY_SCHEMA_SUPPLIER.get();
	}

}
