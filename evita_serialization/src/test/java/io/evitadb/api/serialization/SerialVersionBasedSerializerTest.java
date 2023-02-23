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

package io.evitadb.api.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.AttributeSchemaSerializer;
import io.evitadb.api.serialization.common.SerialVersionBasedSerializer;
import io.evitadb.api.serialization.exception.StoredVersionNotSupportedException;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This test verifies whether it is possible to use special instance for deserializing old versions of the class
 * based on `serialVersionUID` field value.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class SerialVersionBasedSerializerTest {
	private final Kryo kryo = new Kryo();
	private SerialVersionBasedSerializer<AttributeSchema> attributeSchemaSerializer;

	@BeforeEach
	void setUp() {
		attributeSchemaSerializer = new SerialVersionBasedSerializer<>(new AttributeSchemaSerializer(new DefaultKryoSerializationHelper()), AttributeSchema.class);
		kryo.register(AttributeSchema.class, attributeSchemaSerializer);
		kryo.register(OldAttributeSchema.class, new SerialVersionBasedSerializer<>(new OldAttributeSchemaSerializer(), OldAttributeSchema.class));
	}

	@Test
	void shouldFailToDeserializeOldVersion() {
		final ByteArrayOutputStream baos = writeOldFormat();
		try (final ByteBufferInput input = new ByteBufferInput(new ByteArrayInputStream(baos.toByteArray()))) {
			assertThrows(StoredVersionNotSupportedException.class, () -> kryo.readObject(input, AttributeSchema.class));
		}
	}

	@Test
	void shouldDeserializeOldVersion() {
		attributeSchemaSerializer.addBackwardCompatibleSerializer(
				-7465757161178326011L, new OldAttributeSchemaDeserializer()
		);

		final ByteArrayOutputStream baos = writeOldFormat();
		try (final ByteBufferInput input = new ByteBufferInput(new ByteArrayInputStream(baos.toByteArray()))) {
			final AttributeSchema attributeSchema = kryo.readObject(input, AttributeSchema.class);
			assertEquals("A", attributeSchema.getName());
			assertEquals(String.class, attributeSchema.getType());
		}
	}

	@Nonnull
	private ByteArrayOutputStream writeOldFormat() {
		final OldAttributeSchema oldSchema = new OldAttributeSchema("A", String.class);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		try (final ByteBufferOutput output = new ByteBufferOutput(baos)) {
			kryo.writeObject(output, oldSchema);
		}
		return baos;
	}

	@Data
	@Immutable
	private static class OldAttributeSchema implements Serializable {
		private static final long serialVersionUID = -7465757161178326011L;

		private final String name;
		private final Class<? extends Serializable> type;
	}

	private static class OldAttributeSchemaSerializer extends Serializer<OldAttributeSchema> {

		@Override
		public void write(Kryo kryo, Output output, OldAttributeSchema object) {
			kryo.writeObject(output, object.getName());
			kryo.writeClass(output, object.getType());
		}

		@Override
		public OldAttributeSchema read(Kryo kryo, Input input, Class<? extends OldAttributeSchema> type) {
			throw new UnsupportedOperationException("Will not be used in test!");
		}

	}

	private static class OldAttributeSchemaDeserializer extends Serializer<AttributeSchema> {

		@Override
		public void write(Kryo kryo, Output output, AttributeSchema object) {
			throw new UnsupportedOperationException("Will not be used in test!");
		}

		@Override
		public AttributeSchema read(Kryo kryo, Input input, Class<? extends AttributeSchema> type) {
			final String name = kryo.readObject(input, String.class);
			@SuppressWarnings("unchecked") final Class<? extends Serializable> attrType = kryo.readClass(input).getType();
			return new AttributeSchema(name, attrType, false);
		}

	}

}