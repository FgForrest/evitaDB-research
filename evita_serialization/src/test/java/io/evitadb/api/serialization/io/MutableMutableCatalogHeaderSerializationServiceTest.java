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

import io.evitadb.api.serialization.MutableCatalogHeader;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.serialization.mock.Entities;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies {@link MutableCatalogHeaderSerializationService} serialization and deserialization.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class MutableMutableCatalogHeaderSerializationServiceTest {
	private final MutableCatalogHeader header = new MutableCatalogHeader(
		"test", new WritableClassResolver(1000), Entities.PRODUCT
	);

	@Test
	void shouldSerializeAndDeserializeHeader() {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);

		final MutableCatalogHeaderSerializationService headerSerializationService = new MutableCatalogHeaderSerializationService();
		headerSerializationService.serialize(header, baos);

		final byte[] serializedSchema = baos.toByteArray();
		assertNotNull(serializedSchema);
		assertTrue(serializedSchema.length > 0);

		final MutableCatalogHeader deserializedHeader = headerSerializationService.deserialize(new ByteArrayInputStream(serializedSchema));
		assertEquals(header, deserializedHeader);
		assertNotNull(deserializedHeader.getClassResolver().getRegistration(Entities.class));
	}
}