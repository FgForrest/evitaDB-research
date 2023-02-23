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
import io.evitadb.api.serialization.common.SerialVersionBasedSerializer;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.storage.model.storageParts.ComparableReferencedType;
import io.evitadb.storage.model.storageParts.entity.AttributesStoragePart.AttributesSetKey;
import io.evitadb.storage.model.storageParts.index.AttributeKeyWithIndexType;
import io.evitadb.storage.serialization.index.internal.AttributeKeyWithIndexTypeSerializer;
import io.evitadb.storage.serialization.index.internal.AttributesSetKeySerializer;
import io.evitadb.storage.serialization.index.internal.ComparableReferencedTypeSerializer;
import io.evitadb.storage.serialization.index.internal.PriceListAndCurrencyKeySerializer;

import java.util.function.Consumer;

/**
 * This {@link Consumer} implementation takes default Kryo instance and registers additional serializers that are
 * required to (de)serialize {@link io.evitadb.storage.model.CatalogEntityHeader} contents.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ExtendedCatalogHeaderConfigurer implements Consumer<Kryo> {
	public static final ExtendedCatalogHeaderConfigurer INSTANCE = new ExtendedCatalogHeaderConfigurer();
	private final KryoSerializationHelper kryoSerializationHelper;

	private ExtendedCatalogHeaderConfigurer() {
		this.kryoSerializationHelper = new DefaultKryoSerializationHelper();
	}

	public ExtendedCatalogHeaderConfigurer(KryoSerializationHelper kryoSerializationHelper) {
		this.kryoSerializationHelper = kryoSerializationHelper;
	}

	@Override
	public void accept(Kryo kryo) {
		kryo.register(AttributesSetKey.class, new SerialVersionBasedSerializer<>(new AttributesSetKeySerializer(), AttributesSetKey.class), 600);
		kryo.register(PriceIndexKey.class, new SerialVersionBasedSerializer<>(new PriceListAndCurrencyKeySerializer(kryoSerializationHelper), PriceIndexKey.class), 601);
		kryo.register(ComparableReferencedType.class, new SerialVersionBasedSerializer<>(new ComparableReferencedTypeSerializer(kryoSerializationHelper), ComparableReferencedType.class), 602);
		kryo.register(AttributeKeyWithIndexType.class, new SerialVersionBasedSerializer<>(new AttributeKeyWithIndexTypeSerializer(), AttributeKeyWithIndexType.class), 603);
	}

}