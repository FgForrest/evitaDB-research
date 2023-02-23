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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

/**
 * This {@link Serializer} implementation reads/writes {@link Attributes} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class AttributesSerializer extends Serializer<Attributes> {
	private static final ThreadLocal<Map<String, AttributeSchema>> ATTRIBUTE_SCHEMA_SUPPLIER = new ThreadLocal<>();

	/**
	 * This method allows to set context sensitive attribute schema to the deserialization process. This is necessary
	 * to correctly initialize attribute schema for {@link io.evitadb.api.data.structure.Reference} deserialization.
	 *
	 * @param attributeSchema
	 * @param lambda
	 */
	public static <T> T executeWithSupplier(Map<String, AttributeSchema> attributeSchema, Supplier<T> lambda) {
		try {
			Assert.isTrue(
				ATTRIBUTE_SCHEMA_SUPPLIER.get() == null,
				() -> new IllegalStateException("Attribute schema has been already set! This should never happen!")
			);
			ATTRIBUTE_SCHEMA_SUPPLIER.set(attributeSchema);
			return lambda.get();
		} finally {
			ATTRIBUTE_SCHEMA_SUPPLIER.remove();
		}
	}

	@Override
	public void write(Kryo kryo, Output output, Attributes attributes) {
		output.writeVarInt(attributes.getAttributeLocales().size(), true);
		for (Locale locale : attributes.getAttributeLocales()) {
			kryo.writeObject(output, locale);
		}
		output.writeVarInt(attributes.getAttributeValues().size(), true);
		for (AttributeValue attributeValue : attributes.getAttributeValues()) {
			kryo.writeObject(output, attributeValue);
		}
	}

	@Override
	public Attributes read(Kryo kryo, Input input, Class<? extends Attributes> type) {
		final int attributeLocaleCount = input.readVarInt(true);
		final Set<Locale> attributeLocales = new HashSet<>(attributeLocaleCount);
		for (int i = 0; i < attributeLocaleCount; i++) {
			final Locale locale = kryo.readObject(input, Locale.class);
			attributeLocales.add(locale);
		}

		final int attributeCount = input.readVarInt(true);
		final List<AttributeValue> attributeValues = new ArrayList<>(attributeCount);
		for (int i = 0; i < attributeCount; i++) {
			attributeValues.add(kryo.readObject(input, AttributeValue.class));
		}
		final EntitySchema schema = EntitySerializationContext.getEntitySchema();
		return new Attributes(
			schema,
			attributeValues,
			attributeLocales, ofNullable(ATTRIBUTE_SCHEMA_SUPPLIER.get())
			.orElse(schema.getAttributes())
		);
	}

}
