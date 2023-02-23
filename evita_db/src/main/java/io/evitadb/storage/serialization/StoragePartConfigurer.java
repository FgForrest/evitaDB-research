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

package io.evitadb.storage.serialization;

import com.esotericsoftware.kryo.Kryo;
import io.evitadb.api.data.key.CompressiblePriceKey;
import io.evitadb.api.data.key.CompressiblePriceKeySerializer;
import io.evitadb.api.serialization.KeyCompressor;
import io.evitadb.api.serialization.common.EnumNameSerializer;
import io.evitadb.api.serialization.common.SerialVersionBasedSerializer;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.index.histogram.HistogramBucket;
import io.evitadb.index.histogram.HistogramIndex;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.internalId.PriceWithInternalIds;
import io.evitadb.index.range.RangeIndex;
import io.evitadb.index.range.TransactionalRangePoint;
import io.evitadb.storage.model.storageParts.entity.*;
import io.evitadb.storage.model.storageParts.entity.AttributesStoragePart.AttributesSetKey;
import io.evitadb.storage.model.storageParts.index.AttributeIndexStoragePart.AttributeIndexType;
import io.evitadb.storage.model.storageParts.index.*;
import io.evitadb.storage.model.storageParts.schema.EntitySchemaContainer;
import io.evitadb.storage.serialization.entity.*;
import io.evitadb.storage.serialization.index.*;
import io.evitadb.storage.serialization.index.internal.*;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

/**
 * This {@link Consumer} implementation takes default Kryo instance and registers additional serializers that are
 * required to (de)serialize {@link io.evitadb.storage.model.storageParts.EntityStoragePart} implementations.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class StoragePartConfigurer implements Consumer<Kryo> {
	private final KryoSerializationHelper kryoSerializationHelper;
	private final KeyCompressor keyCompressor;

	@Override
	public void accept(Kryo kryo) {
		kryo.register(EntitySchemaContainer.class, new SerialVersionBasedSerializer<>(new EntitySchemaPartSerializer(), EntitySchemaContainer.class), 500);

		kryo.register(EntityBodyStoragePart.class, new SerialVersionBasedSerializer<>(new EntityBodyStoragePartSerializer(keyCompressor), EntityBodyStoragePart.class), 501);
		kryo.register(PricesStoragePart.class, new SerialVersionBasedSerializer<>(new PricesStoragePartSerializer(), PricesStoragePart.class), 502);
		kryo.register(ReferencesStoragePart.class, new SerialVersionBasedSerializer<>(new ReferencesStoragePartSerializer(), ReferencesStoragePart.class), 503);
		kryo.register(AttributesStoragePart.class, new SerialVersionBasedSerializer<>(new AttributesStoragePartSerializer(keyCompressor), AttributesStoragePart.class), 504);
		kryo.register(AttributesSetKey.class, new SerialVersionBasedSerializer<>(new AttributesSetKeySerializer(), AttributesSetKey.class), 505);
		kryo.register(AssociatedDataStoragePart.class, new SerialVersionBasedSerializer<>(new AssociatedDataStoragePartSerializer(keyCompressor), AssociatedDataStoragePart.class), 506);

		kryo.register(EntityIndexStoragePart.class, new SerialVersionBasedSerializer<>(new EntityIndexStoragePartSerializer(kryoSerializationHelper, keyCompressor), EntityIndexStoragePart.class), 520);
		kryo.register(UniqueIndexStoragePart.class, new SerialVersionBasedSerializer<>(new UniqueIndexStoragePartSerializer(keyCompressor), UniqueIndexStoragePart.class), 521);
		kryo.register(FilterIndexStoragePart.class, new SerialVersionBasedSerializer<>(new FilterIndexStoragePartSerializer(keyCompressor), FilterIndexStoragePart.class), 522);
		kryo.register(SortIndexStoragePart.class, new SerialVersionBasedSerializer<>(new SortIndexStoragePartSerializer(keyCompressor), SortIndexStoragePart.class), 523);
		kryo.register(HistogramIndex.class, new SerialVersionBasedSerializer<>(new HistogramIndexSerializer(), HistogramIndex.class), 524);
		kryo.register(HistogramBucket.class, new SerialVersionBasedSerializer<>(new HistogramBucketSerializer(), HistogramBucket.class), 525);
		kryo.register(RangeIndex.class, new SerialVersionBasedSerializer<>(new IntRangeIndexSerializer(), RangeIndex.class), 526);
		kryo.register(TransactionalRangePoint.class, new SerialVersionBasedSerializer<>(new TransactionalIntRangePointSerializer(), TransactionalRangePoint.class), 527);
		kryo.register(EntityIndexType.class, new EnumNameSerializer<>(kryoSerializationHelper), 528);
		kryo.register(AttributeIndexType.class, new EnumNameSerializer<>(kryoSerializationHelper), 529);
		kryo.register(TransactionalBitmap.class, new SerialVersionBasedSerializer<>(new TransactionalIntegerBitmapSerializer(), TransactionalBitmap.class), 530);
		kryo.register(CompressiblePriceKey.class, new CompressiblePriceKeySerializer(kryoSerializationHelper), 531);
		kryo.register(PriceIndexKey.class, new SerialVersionBasedSerializer<>(new PriceIndexKeySerializer(kryoSerializationHelper), PriceIndexKey.class), 532);
		kryo.register(PriceListAndCurrencySuperIndexStoragePart.class, new SerialVersionBasedSerializer<>(new PriceListAndCurrencySuperIndexStoragePartSerializer(keyCompressor), PriceListAndCurrencySuperIndexStoragePart.class), 533);
		kryo.register(PriceListAndCurrencyRefIndexStoragePart.class, new SerialVersionBasedSerializer<>(new PriceListAndCurrencyRefIndexStoragePartSerializer(keyCompressor), PriceListAndCurrencyRefIndexStoragePart.class), 534);
		kryo.register(HierarchyIndexStoragePart.class, new SerialVersionBasedSerializer<>(new HierarchyIndexStorgePartSerializer(), HierarchyIndexStoragePart.class), 535);
		kryo.register(FacetIndexStoragePart.class, new SerialVersionBasedSerializer<>(new FacetIndexStoragePartSerializer(keyCompressor), FacetIndexStoragePart.class), 536);
		kryo.register(PriceWithInternalIds.class, new SerialVersionBasedSerializer<>(new PriceWithInternalIdsSerializer(keyCompressor), PriceWithInternalIds.class), 537);
	}

}