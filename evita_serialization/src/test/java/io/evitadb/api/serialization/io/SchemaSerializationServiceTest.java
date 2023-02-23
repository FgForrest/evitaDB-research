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

package io.evitadb.api.serialization.io;

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.api.serialization.MutableCatalogEntityHeader;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.serialization.mock.DataGenerator;
import io.evitadb.api.serialization.mock.Entities;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies {@link SchemaSerializationService} serialization and deserialization.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class SchemaSerializationServiceTest {
	private final MutableCatalogEntityHeader header = new MutableCatalogEntityHeader(
		Entities.PRODUCT, new WritableClassResolver(1000), 0, Collections.emptyMap()
	);

	@Test
	void shouldSerializeAndDeserializeSchema() {
		final EntitySchema productSchema = new EntitySchema(Entities.PRODUCT);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		final SchemaSerializationService schemaSerializationService = new SchemaSerializationService(header);
		final EntitySchema createdSchema = DataGenerator.constructSomeSchema(
				new EntitySchemaBuilder(
						productSchema,
						schema -> {
							schemaSerializationService.serialize(schema, baos);
							return schema;
						}
				)
		);

		final byte[] serializedSchema = baos.toByteArray();
		assertNotNull(serializedSchema);
		assertTrue(serializedSchema.length > 0);

		final EntitySchema deserializedSchema = schemaSerializationService.deserialize(new ByteArrayInputStream(serializedSchema));
		assertEquals(createdSchema, deserializedSchema);
		assertFalse(createdSchema.differsFrom(deserializedSchema));
	}

}