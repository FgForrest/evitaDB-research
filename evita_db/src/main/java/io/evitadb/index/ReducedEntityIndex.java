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

package io.evitadb.index;

import io.evitadb.api.EntityCollection;
import io.evitadb.api.Transaction;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.index.attribute.AttributeIndex;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.index.facet.FacetIndex;
import io.evitadb.index.hierarchy.HierarchyIndex;
import io.evitadb.index.price.PriceIndexContract;
import io.evitadb.index.price.PriceRefIndex;
import io.evitadb.index.price.PriceSuperIndex;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalMemory;
import io.evitadb.index.transactionalMemory.VoidTransactionMemoryProducer;
import io.evitadb.query.algebra.Formula;
import io.evitadb.storage.model.storageParts.StoragePart;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reduced entity index is a "helper" index that maintains primarily bitmaps of primary keys that are connected to
 * a limited scope view of the data. All memory expensive objects are referred and maintained in {@link GlobalEntityIndex}
 * so that it's ensured they exist solely on the heap.
 *
 * Reduced indexes are used for handling queries that target {@link io.evitadb.api.data.ReferenceContract}
 * of the entities. In such case we may prefer using data from reduced entity index because it may substantially limit
 * the amount of operations to answer the query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class ReducedEntityIndex extends EntityIndex implements VoidTransactionMemoryProducer<ReducedEntityIndex> {
	/**
	 * This part of index collects information about prices of the entities. It provides data that are necessary for
	 * constructing {@link Formula} tree for the constraints related to the prices.
	 */
	@Delegate(types = PriceIndexContract.class)
	@Getter private final PriceRefIndex priceIndex;

	public ReducedEntityIndex(int primaryKey, @Nonnull EntityIndexKey entityIndexKey, @Nonnull Supplier<EntitySchema> schemaAccessor, @Nonnull Supplier<PriceSuperIndex> superIndexAccessor) {
		super(primaryKey, entityIndexKey, schemaAccessor);
		this.priceIndex = new PriceRefIndex(superIndexAccessor);
	}

	public ReducedEntityIndex(int primaryKey, @Nonnull EntityIndexKey entityIndexKey, int version, @Nonnull Supplier<EntitySchema> schemaAccessor, @Nonnull Bitmap entityIds, @Nonnull Map<Locale, TransactionalBitmap> entityIdsByLanguage, @Nonnull AttributeIndex attributeIndex, @Nonnull PriceRefIndex priceIndex, @Nonnull HierarchyIndex hierarchyIndex, @Nonnull FacetIndex facetIndex) {
		super(primaryKey, entityIndexKey, version, schemaAccessor, entityIds, entityIdsByLanguage, attributeIndex, hierarchyIndex, facetIndex);
		this.priceIndex = priceIndex;
	}

	@Override
	public void updateReferencesTo(EntityCollection newCollection) {
		super.updateReferencesTo(newCollection);
		this.priceIndex.updateReferencesTo(newCollection::getPriceSuperIndex);
	}

	@Override
	public boolean isEmpty() {
		return super.isEmpty() && this.priceIndex.isPriceIndexEmpty();
	}

	@Override
	public Collection<StoragePart> getModifiedStorageParts() {
		final Collection<StoragePart> dirtyList = super.getModifiedStorageParts();
		dirtyList.addAll(this.priceIndex.getModifiedStorageParts(this.primaryKey));
		return dirtyList;
	}

	@Override
	public void resetDirty() {
		super.resetDirty();
		this.priceIndex.resetDirty();
	}

	@Override
	public void clearTransactionalMemory() {
		super.clearTransactionalMemory();
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this);
		this.priceIndex.clearTransactionalMemory();
	}

	@Override
	public ReducedEntityIndex createCopyWithMergedTransactionalMemory(@Nullable Void layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		// we can safely throw away dirty flag now
		transactionalLayer.removeTransactionalMemoryLayerIfExists(this.dirty);
		return new ReducedEntityIndex(
			primaryKey, entityIndexKey, version + 1, schemaAccessor,
			transactionalLayer.getStateCopyWithCommittedChanges(this.entityIds, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.entityIdsByLanguage, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.attributeIndex, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.priceIndex, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.hierarchyIndex, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.facetIndex, transaction)
		);
	}


}
