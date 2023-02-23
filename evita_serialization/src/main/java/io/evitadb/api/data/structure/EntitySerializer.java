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
import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Supplier;

/**
 * This {@link Serializer} implementation reads/writes {@link Entity} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class EntitySerializer extends Serializer<Entity> {
	private final Supplier<EntitySchema> schemaSupplier;
	private final ReflectionLookup reflectionLookup;

	@Override
	public void write(Kryo kryo, Output output, Entity entity) {
		output.writeVarInt(entity.getVersion(), true);
		output.writeBoolean(entity.isDropped());
		output.writeVarInt(Objects.requireNonNull(entity.getPrimaryKey()), true);
		kryo.writeObjectOrNull(output, entity.getHierarchicalPlacement(), HierarchicalPlacement.class);
		kryo.writeObject(output, entity.attributes);
		output.writeBoolean(entity.associatedData != null);
		if (entity.associatedData != null) {
			kryo.writeObject(output, entity.associatedData);
		}
		kryo.writeObject(output, entity.prices);
		output.writeVarInt(entity.locales.size(), true);
		for (Locale locale : entity.locales) {
			kryo.writeObject(output, locale);
		}
		output.writeVarInt(entity.references.size(), true);
		for (ReferenceContract reference : entity.references.values()) {
			kryo.writeObject(output, reference);
		}
	}

	@Override
	public Entity read(Kryo kryo, Input input, Class<? extends Entity> type) {
		final EntitySchema schema = schemaSupplier.get();
		return EntitySerializationContext.executeWithSupplier(schema, () -> {
			final int version = input.readVarInt(true);
			final boolean dropped = input.readBoolean();
			final int primaryKey = input.readVarInt(true);
			final HierarchicalPlacement hierarchicalPlacement = kryo.readObjectOrNull(input, HierarchicalPlacement.class);
			final Attributes attributes = kryo.readObject(input, Attributes.class);
			final boolean associatedDataExists = input.readBoolean();
			final AssociatedData associatedData;
			if (associatedDataExists) {
				associatedData = kryo.readObject(input, AssociatedData.class);
			} else {
				associatedData = new AssociatedData(schema, reflectionLookup);
			}
			final Prices prices = kryo.readObject(input, Prices.class);
			final int localeCount = input.readVarInt(true);
			final Set<Locale> locales = new HashSet<>(localeCount);
			for (int i = 0; i < localeCount; i++) {
				locales.add(kryo.readObject(input, Locale.class));
			}
			final int referenceCount = input.readVarInt(true);
			final List<ReferenceContract> references = new ArrayList<>(referenceCount);
			for (int i = 0; i < referenceCount; i++) {
				references.add(kryo.readObject(input, Reference.class));
			}
			return new Entity(
				version, schema, primaryKey,
				hierarchicalPlacement, references, attributes, associatedData,
				prices, locales, dropped
			);
		});
	}

}
