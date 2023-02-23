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

package io.evitadb.storage.serialization.index;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.key.CompressiblePriceKey;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.storage.model.storageParts.ComparableReferencedType;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStorageKey;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStoragePart.AttributeIndexType;
import io.evitadb.storage.model.storageParts.index.EntityIndexStoragePart;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static io.evitadb.api.utils.CollectionUtils.createHashMap;

/**
 * This {@link Serializer} implementation reads/writes {@link EntityIndex} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class EntityIndexStoragePartSerializer extends Serializer<EntityIndexStoragePart> {
	private final KryoSerializationHelper kryoSerializationHelper;
	private final KeyCompressor keyCompressor;

	@Override
	public void write(Kryo kryo, Output output, EntityIndexStoragePart entityIndex) {
		output.writeVarInt(entityIndex.getPrimaryKey(), true);
		output.writeVarInt(entityIndex.getVersion(), true);

		final EntityIndexKey entityIndexKey = entityIndex.getEntityIndexKey();
		kryo.writeObject(output, entityIndexKey.getType());
		kryoSerializationHelper.writeOptionalSerializable(kryo, output, entityIndexKey.getDiscriminator());

		final Bitmap entityIds = entityIndex.getEntityIds();
		kryo.writeObject(output, entityIds);

		final Map<Locale, ? extends Bitmap> entitiesIdsByLanguage = entityIndex.getEntitiesIdsByLanguage();
		output.writeVarInt(entitiesIdsByLanguage.size(), true);
		for (Entry<Locale, ? extends Bitmap> entry : entitiesIdsByLanguage.entrySet()) {
			kryo.writeObject(output, entry.getKey());
			kryo.writeObject(output, entry.getValue());
		}

		final Set<AttributeIndexStorageKey> attributeIndexes = entityIndex.getAttributeIndexes();
		output.writeVarInt(attributeIndexes.size(), true);
		for (AttributeIndexStorageKey attributeIndexKey : attributeIndexes) {
			kryo.writeObject(output, attributeIndexKey.getIndexType());
			output.writeVarInt(keyCompressor.getId(attributeIndexKey.getAttribute()), true);
		}

		kryo.writeObjectOrNull(output, entityIndex.getInternalPriceIdSequence(), Integer.class);

		final Set<PriceIndexKey> priceIndexes = entityIndex.getPriceIndexes();
		output.writeVarInt(priceIndexes.size(), true);
		for (PriceIndexKey priceIndexKey : priceIndexes) {
			final CompressiblePriceKey cpk = new CompressiblePriceKey(priceIndexKey.getPriceList(), priceIndexKey.getCurrency());
			output.writeVarInt(keyCompressor.getId(cpk), true);
			output.writeVarInt(priceIndexKey.getRecordHandling().ordinal(), true);
		}

		output.writeBoolean(entityIndex.isHierarchyIndex());

		final Set<Serializable> facetIndexes = entityIndex.getFacetIndexes();
		output.writeVarInt(facetIndexes.size(), true);
		for (Serializable referencedEntity : facetIndexes) {
			final ComparableReferencedType crt = new ComparableReferencedType(referencedEntity);
			output.writeVarInt(keyCompressor.getId(crt), true);
		}
	}

	@Override
	public EntityIndexStoragePart read(Kryo kryo, Input input, Class<? extends EntityIndexStoragePart> type) {
		final int primaryKey = input.readVarInt(true);
		final int version = input.readVarInt(true);

		final EntityIndexType entityIndexType = kryo.readObject(input, EntityIndexType.class);
		final Serializable discriminator = kryoSerializationHelper.readOptionalSerializable(kryo, input);
		final EntityIndexKey entityIndexKey = new EntityIndexKey(entityIndexType, discriminator);

		final TransactionalBitmap entityIds = kryo.readObject(input, TransactionalBitmap.class);

		final int languageCount = input.readVarInt(true);
		final Map<Locale, TransactionalBitmap> entityIdsByLocale = createHashMap(languageCount);
		for (int i = 0; i < languageCount; i++) {
			final Locale locale = kryo.readObject(input, Locale.class);
			final TransactionalBitmap localeSpecificEntityIds = kryo.readObject(input, TransactionalBitmap.class);
			entityIdsByLocale.put(locale, localeSpecificEntityIds);
		}

		final int attributeIndexesCount = input.readVarInt(true);
		final Set<AttributeIndexStorageKey> attributeIndexes = new HashSet<>(attributeIndexesCount);
		for (int i = 0; i < attributeIndexesCount; i++) {
			final AttributeIndexType attributeIndexType = kryo.readObject(input, AttributeIndexType.class);
			final AttributeKey attributeKey = keyCompressor.getKeyForId(input.readVarInt(true));
			attributeIndexes.add(new AttributeIndexStorageKey(entityIndexKey, attributeIndexType, attributeKey));
		}

		final Integer internalPriceIdSequenceSeed = kryo.readObjectOrNull(input, Integer.class);

		final int priceIndexesCount = input.readVarInt(true);
		final Set<PriceIndexKey> priceIndexes = new HashSet<>(priceIndexesCount);
		for (int i = 0; i < priceIndexesCount; i++) {
			final CompressiblePriceKey priceKey = keyCompressor.getKeyForId(input.readVarInt(true));
			final PriceInnerRecordHandling innerRecordHandling = PriceInnerRecordHandling.values()[input.readVarInt(true)];
			priceIndexes.add(
				new PriceIndexKey(priceKey.getPriceList(), priceKey.getCurrency(), innerRecordHandling)
			);
		}

		final boolean hierarchyIndex = input.readBoolean();

		final int facetIndexesCount = input.readVarInt(true);
		final Set<Serializable> facetIndexes = new HashSet<>(facetIndexesCount);
		for (int i = 0; i < facetIndexesCount; i++) {
			final ComparableReferencedType crt = keyCompressor.getKeyForId(input.readVarInt(true));
			facetIndexes.add(crt.getEntityType());
		}

		return new EntityIndexStoragePart(
			primaryKey, version, entityIndexKey, entityIds, entityIdsByLocale, attributeIndexes,
			internalPriceIdSequenceSeed, priceIndexes,
			hierarchyIndex, facetIndexes
		);
	}
}
