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

package io.evitadb.index.attribute;

import io.evitadb.api.Transaction;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.exception.UniqueValueViolationException;
import io.evitadb.index.EntityIndexDataStructure;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.TransactionalBitmap;
import io.evitadb.index.bool.TransactionalBoolean;
import io.evitadb.index.map.MapChanges;
import io.evitadb.index.map.TransactionalMemoryMap;
import io.evitadb.index.transactionalMemory.*;
import io.evitadb.storage.model.storageParts.StoragePart;
import io.evitadb.storage.model.storageParts.index.UniqueIndexStoragePart;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.StringUtils.unknownToString;
import static java.util.Optional.ofNullable;

/**
 * Unique index maintains information about single unique attribute - its value to record id relation.
 * It protects duplicate unique attribute insertion and allows to easily translate unique attribute value to record id
 * that occupies it.
 * <p>
 * It uses simple {@link HashMap} data structure to keep the data. This means that look-ups are retrieved with O(1)
 * complexity.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
public class UniqueIndex implements TransactionalLayerProducer<TransactionalContainerChanges<MapChanges<Serializable, Integer>, Map<Serializable, Integer>, TransactionalMemoryMap<Serializable, Integer>>, UniqueIndex>, EntityIndexDataStructure, Serializable {
	private static final long serialVersionUID = 2639205026498958516L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	/**
	 * Contains name of the attribute.
	 */
	@Getter private final String name;
	/**
	 * Contains type of the attribute.
	 */
	@Getter private final Class<? extends Serializable> type;
	/**
	 * This is internal flag that tracks whether the index contents became dirty and needs to be persisted.
	 */
	private final TransactionalBoolean dirty;
	/**
	 * Keeps the unique value to record id mappings. Fairly large HashMap is expected here.
	 */
	private final TransactionalMemoryMap<Serializable, Integer> uniqueValueToRecordId;
	/**
	 * Keeps information about all record ids resent in this index.
	 */
	private final TransactionalBitmap recordIds;

	public UniqueIndex(String attributeName, Class<? extends Serializable> attributeType) {
		this.dirty = new TransactionalBoolean();
		this.name = attributeName;
		this.type = attributeType;
		this.uniqueValueToRecordId = new TransactionalMemoryMap<>(new HashMap<>());
		this.recordIds = new TransactionalBitmap();
	}

	public UniqueIndex(String attributeName, Class<? extends Serializable> attributeType, Map<Serializable, Integer> uniqueValueToRecordId) {
		this.dirty = new TransactionalBoolean();
		this.name = attributeName;
		this.type = attributeType;
		this.uniqueValueToRecordId = new TransactionalMemoryMap<>(new HashMap<>(uniqueValueToRecordId));
		this.recordIds = new TransactionalBitmap(uniqueValueToRecordId.values().stream().mapToInt(it -> it).toArray());
	}

	UniqueIndex(String attributeName, Class<? extends Serializable> attributeType, Map<Serializable, Integer> uniqueValueToRecordId, Bitmap recordIds) {
		this.dirty = new TransactionalBoolean();
		this.name = attributeName;
		this.type = attributeType;
		this.uniqueValueToRecordId = new TransactionalMemoryMap<>(uniqueValueToRecordId);
		this.recordIds = new TransactionalBitmap(recordIds);
	}

	/**
	 * Registers new record id to a single unique value.
	 *
	 * @throws UniqueValueViolationException when value is not unique
	 */
	public void registerUniqueKey(Object value, int recordId) {
		registerUniqueKeyValue(value, recordId);
	}

	/**
	 * Unregisters new record id from a single unique value.
	 *
	 * @return removed record id relation
	 */
	public int unregisterUniqueKey(Object value, int recordId) {
		return unregisterUniqueKeyValue(value, recordId);
	}

	/**
	 * Returns record id by its unique value.
	 */
	@Nullable
	public Integer getRecordIdByUniqueValue(Serializable value) {
		return this.uniqueValueToRecordId.get(value);
	}

	/**
	 * Returns collection of all unique keys in this index.
	 */
	public Collection<Serializable> getUniqueValues() {
		return this.uniqueValueToRecordId.keySet();
	}

	/**
	 * Returns index of unique values mapped to record ids.
	 */
	public Map<Serializable, Integer> getUniqueValueToRecordId() {
		return Collections.unmodifiableMap(uniqueValueToRecordId);
	}

	/**
	 * Returns bitmap with all record ids registered in this unique index.
	 */
	@Nonnull
	public Bitmap getRecordIds() {
		return recordIds;
	}

	/**
	 * Returns number of records in this index.
	 */
	public int size() {
		return recordIds.size();
	}

	/**
	 * Method creates container for storing unique index from memory to the persistent storage.
	 */
	@Nullable
	public StoragePart createStoragePart(int entityIndexPrimaryKey, AttributeKey attribute) {
		if (this.dirty.isTrue()) {
			return new UniqueIndexStoragePart(entityIndexPrimaryKey, attribute, type, uniqueValueToRecordId, recordIds);
		} else {
			return null;
		}
	}

	@Override
	public void resetDirty() {
		this.dirty.reset();
	}

	@Override
	public void clearTransactionalMemory() {
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.dirty);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.uniqueValueToRecordId);
		TransactionalMemory.removeTransactionalMemoryLayerIfExists(this.recordIds);
	}

	/*
		TransactionalLayerCreator implementation
	 */

	@Override
	public TransactionalContainerChanges<MapChanges<Serializable, Integer>, Map<Serializable, Integer>, TransactionalMemoryMap<Serializable, Integer>> createLayer() {
		return TransactionalMemory.isTransactionalMemoryAvailable() ? new TransactionalContainerChanges<>() : null;
	}

	@Override
	public UniqueIndex createCopyWithMergedTransactionalMemory(@Nullable TransactionalContainerChanges<MapChanges<Serializable, Integer>, Map<Serializable, Integer>, TransactionalMemoryMap<Serializable, Integer>> layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, Transaction transaction) {
		final UniqueIndex uniqueKeyIndex = new UniqueIndex(
			name, type,
			transactionalLayer.getStateCopyWithCommittedChanges(this.uniqueValueToRecordId, transaction),
			transactionalLayer.getStateCopyWithCommittedChanges(this.recordIds, transaction)
		);
		transactionalLayer.removeTransactionalMemoryLayerIfExists(this.dirty);
		// we can safely throw away dirty flag now
		ofNullable(layer).ifPresent(it -> it.clean(transactionalLayer));
		return uniqueKeyIndex;
	}

	public boolean isEmpty() {
		return this.uniqueValueToRecordId.isEmpty();
	}

	/*
		PRIVATE METHODS
	 */

	private static void verifyValueArray(@Nonnull Object value) {
		isTrue(Serializable.class.isAssignableFrom(value.getClass().getComponentType()), () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Serializable but it is not!"));
		isTrue(Comparable.class.isAssignableFrom(value.getClass().getComponentType()), () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Comparable but it is not!"));
	}

	private static void verifyValue(@Nonnull Object value) {
		isTrue(value instanceof Serializable, () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Serializable but it is not!"));
		isTrue(value instanceof Comparable, () -> new IllegalArgumentException("Value `" + unknownToString(value) + "` is expected to be Comparable but it is not!"));
	}

	@SuppressWarnings("unchecked")
	private <T extends Serializable & Comparable<T>> void registerUniqueKeyValue(Object key, int recordId) {
		if (key instanceof Object[]) {
			verifyValueArray(key);
			final Object[] valueArray = (Object[]) key;
			// first verify removed data without modifications
			for (Object valueItem : valueArray) {
				final T theValueItem = (T) valueItem;
				final Integer existingRecordId = this.uniqueValueToRecordId.get(theValueItem);
				assertUniqueKeyIsFree(theValueItem, recordId, existingRecordId);
			}
			// now perform alteration
			for (Object valueItem : valueArray) {
				//noinspection unchecked
				registerUniqueKeyValue((T) valueItem, recordId);
			}
		} else {
			verifyValue(key);
			//noinspection unchecked
			registerUniqueKeyValue((T) key, recordId);
		}
		this.dirty.setToTrue();
	}

	private <T extends Serializable & Comparable<T>> void registerUniqueKeyValue(T key, int recordId) {
		final Integer existingRecordId = uniqueValueToRecordId.get(key);
		assertUniqueKeyIsFree(key, recordId, existingRecordId);
		this.uniqueValueToRecordId.put(key, recordId);
		this.recordIds.add(recordId);
	}

	@SuppressWarnings("unchecked")
	private <T extends Serializable & Comparable<T>> int unregisterUniqueKeyValue(Object key, int expectedRecordId) {
		if (key instanceof Object[]) {
			verifyValueArray(key);
			final Object[] valueArray = (Object[]) key;
			// first verify removed data without modifications
			for (Object valueItem : valueArray) {
				final T theValueItem = (T) valueItem;
				final Integer existingRecordId = this.uniqueValueToRecordId.get(theValueItem);
				assertUniqueKeyOwnership(theValueItem, expectedRecordId, existingRecordId);
			}
			// now perform alteration
			for (Object valueItem : valueArray) {
				unregisterUniqueKeyValue((T) valueItem, expectedRecordId);
			}
			this.dirty.setToTrue();
			return Integer.MIN_VALUE;
		} else {
			verifyValue(key);
			final int originalValue = unregisterUniqueKeyValue((T) key, expectedRecordId);
			this.dirty.setToTrue();
			return originalValue;
		}
	}

	private <T extends Serializable & Comparable<T>> int unregisterUniqueKeyValue(T key, int expectedRecordId) {
		final Integer existingRecordId = this.uniqueValueToRecordId.remove(key);
		assertUniqueKeyOwnership(key, expectedRecordId, existingRecordId);
		this.recordIds.remove(existingRecordId);
		return existingRecordId;
	}

	private <T extends Serializable & Comparable<T>> void assertUniqueKeyIsFree(T key, int recordId, Integer existingRecordId) {
		if (!(existingRecordId == null || existingRecordId.equals(recordId))) {
			throw new UniqueValueViolationException(name, key, existingRecordId, recordId);
		}
	}

	private <T extends Serializable & Comparable<T>> void assertUniqueKeyOwnership(T key, int expectedRecordId, Integer existingRecordId) {
		isTrue(
			Objects.equals(existingRecordId, expectedRecordId),
			() -> existingRecordId == null ?
				new IllegalArgumentException("No unique key exists for `" + name + "` key: `" + key + "`!") :
				new IllegalArgumentException("Unique key exists for `" + name + "` key: `" + key + "` belongs to record with id `" + existingRecordId + "` and not `" + expectedRecordId + "` as expected!")
		);
	}

}
