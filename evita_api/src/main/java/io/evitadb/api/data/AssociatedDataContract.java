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

package io.evitadb.api.data;

import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.MemoryMeasuringConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static io.evitadb.api.utils.ComparatorUtils.compareLocale;

/**
 * This interface prescribes a set of methods that must be implemented by the object, that maintains set of associated data.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public interface AssociatedDataContract extends Serializable {

	/**
	 * Returns true if single associated data differs between first and second instance.
	 */
	static boolean anyAssociatedDataDifferBetween(AssociatedDataContract first, AssociatedDataContract second) {
		final Collection<AssociatedDataValue> thisValues = first.getAssociatedDataValues();
		final Collection<AssociatedDataValue> otherValues = second.getAssociatedDataValues();

		if (thisValues.size() != otherValues.size()) {
			return true;
		} else {
			return first.getAssociatedDataValues()
				.stream()
				.anyMatch(it -> {
					final AssociatedDataKey key = it.getKey();
					final Serializable thisValue = it.getValue();
					final Serializable otherValue = second.getAssociatedData(
						key.getAssociatedDataName(), key.getLocale()
					);
					return QueryUtils.valueDiffers(thisValue, otherValue);
				});
		}
	}

	/**
	 * Returns value associated with the key or null when the associatedData is missing.
	 *
	 * @throws ClassCastException when associatedData is of different type than expected
	 */
	@Nullable
	<T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName);

	/**
	 * Returns value associated with the key or null when the associatedData is missing.
	 *
	 * @throws ClassCastException                                          when associatedData is of different type than expected
	 * @throws io.evitadb.api.exception.IncompleteDeserializationException when only part of the complex object data was deserialized
	 */
	@Nullable
	<T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, Class<T> dtoType);

	/**
	 * Returns array of values associated with the key or null when the associatedData is missing.
	 *
	 * @throws ClassCastException when associatedData is of different type than expected or is not an array
	 */
	@Nullable
	<T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName);

	/**
	 * Returns array of values associated with the key or null when the associated data is missing.
	 *
	 * Method returns wrapper dto for the associated data that contains information about the associated data version
	 * and state.
	 */
	@Nullable
	AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName);

	/**
	 * Returns value associated with the key or null when the associatedData is missing.
	 * When localized associatedData is not found it is looked up in generic (non-localized) associatedDatas. This makes this
	 * method safest way how to lookup for associatedData if caller doesn't know whether it is localized or not.
	 *
	 * @throws ClassCastException when associatedData is of different type than expected
	 */
	@Nullable
	<T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale);

	/**
	 * Returns value associated with the key or null when the associatedData is missing.
	 *
	 * @throws ClassCastException                                          when associatedData is of different type than expected
	 * @throws io.evitadb.api.exception.IncompleteDeserializationException when only part of the complex object data was deserialized
	 */
	@Nullable
	<T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, Class<T> dtoType);

	/**
	 * Returns array of values associated with the key or null when the associatedData is missing.
	 * When localized associatedData is not found it is looked up in generic (non-localized) associatedDatas. This makes this
	 * method safest way how to lookup for associatedData if caller doesn't know whether it is localized or not.
	 *
	 * @throws ClassCastException when associatedData is of different type than expected or is not an array
	 */
	@Nullable
	<T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName, @Nonnull Locale locale);

	/**
	 * Returns array of values associated with the key or null when the associated data is missing.
	 * When localized associated data is not found it is looked up in generic (non-localized) associated data. This
	 * makes this method safest way how to lookup for associated data if caller doesn't know whether it is localized
	 * or not.
	 *
	 * Method returns wrapper dto for the associated data that contains information about the associated data version
	 * and state.
	 */
	@Nullable
	AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName, @Nonnull Locale locale);

	/**
	 * Returns definition for the associatedData of specified name.
	 */
	@Nullable
	AssociatedDataSchema getAssociatedDataSchema(@Nonnull String associatedDataName);

	/**
	 * Returns set of all keys registered in this associatedData set. The result set is not limited to the set
	 * of currently fetched associated data.
	 */
	@Nonnull
	Set<String> getAssociatedDataNames();

	/**
	 * Returns set of all keys (combination of associated data name and locale) registered in this associated data.
	 */
	@Nonnull
	Set<AssociatedDataKey> getAssociatedDataKeys();

	/**
	 * Returns collection of all values present in this object.
	 */
	@Nonnull
	Collection<AssociatedDataValue> getAssociatedDataValues();

	/**
	 * Returns collection of all values of `associatedDataName` present in this object. This method has usually sense
	 * only when the associated data is present in multiple localizations.
	 */
	@Nonnull
	Collection<AssociatedDataValue> getAssociatedDataValues(@Nonnull String associatedDataName);

	/**
	 * Method returns set of locales used in the localized associated data. The result set is not limited to the set
	 * of currently fetched associated data.
	 */
	@Nonnull
	Set<Locale> getAssociatedDataLocales();

	/**
	 * Inner implementation used in {@link AssociatedDataContract} to represent a proper key in hash map.
	 *
	 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
	 */
	@Immutable
	@ThreadSafe
	@Data
	class AssociatedDataKey implements Serializable, Comparable<AssociatedDataKey> {
		private static final long serialVersionUID = -8516513307116598241L;

		/**
		 * Unique name of the associatedData. Case sensitive. Distinguishes one associated data item from another within
		 * single entity instance.
		 */
		private final String associatedDataName;
		/**
		 * Contains locale in case the associatedData is locale specific (i.e. {@link AssociatedDataSchema#isLocalized()}
		 */
		private final Locale locale;

		/**
		 * Construction for the locale agnostics associatedData.
		 */
		public AssociatedDataKey(String associatedDataName) {
			Assert.notNull(associatedDataName, "AssociatedData name cannot be null!");
			this.associatedDataName = associatedDataName;
			this.locale = null;
		}

		/**
		 * Constructor for the locale specific associatedData.
		 */
		public AssociatedDataKey(String associatedDataName, Locale locale) {
			Assert.notNull(associatedDataName, "AssociatedData name cannot be null!");
			this.associatedDataName = associatedDataName;
			this.locale = locale;
		}

		/**
		 * Returns true if associatedData is localized.
		 */
		public boolean isLocalized() {
			return locale != null;
		}

		@Override
		public int compareTo(@Nonnull AssociatedDataKey o) {
			return compareLocale(locale, o.locale, () -> associatedDataName.compareTo(o.associatedDataName));
		}

		/**
		 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
		 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
		 */
		public int estimateSize() {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE +
				// data name
				MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.computeStringSize(associatedDataName) +
				// locale
				MemoryMeasuringConstants.REFERENCE_SIZE;
		}
	}

	/**
	 * Associated data holder is used internally to keep track of associated data as well as its unique identification that
	 * is assigned new everytime associated data changes somehow.
	 *
	 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
	 */
	@Immutable
	@ThreadSafe
	@Data
	@EqualsAndHashCode(of = {"version", "key"})
	class AssociatedDataValue implements Versioned, Droppable, Serializable, ContentComparator<AssociatedDataValue> {
		private static final long serialVersionUID = -4732226749198696974L;

		/**
		 * Contains version of this object and gets increased with any associatedData update. Allows to execute
		 * optimistic locking i.e. avoiding parallel modifications.
		 */
		private final int version;
		/**
		 * Uniquely identifies the associated data value among other associated data in the same entity instance.
		 */
		private final AssociatedDataKey key;
		/**
		 * Returns associated data value contents.
		 */
		private final Serializable value;
		/**
		 * Contains TRUE if associatedData was dropped - i.e. removed. Such associated data are not removed (unless tidying process
		 * does it), but are lying among other associated data with tombstone flag. Dropped associated data can be overwritten by
		 * a new value continuing with the versioning where it was stopped for the last time.
		 */
		private final boolean dropped;

		public AssociatedDataValue(AssociatedDataKey key, Serializable value) {
			this.version = 1;
			this.key = key;
			this.value = value;
			this.dropped = false;
		}

		public AssociatedDataValue(int version, AssociatedDataKey key, Serializable value) {
			this.version = version;
			this.key = key;
			this.value = value;
			this.dropped = false;
		}

		public AssociatedDataValue(int version, AssociatedDataKey key, Serializable value, boolean dropped) {
			this.version = version;
			this.key = key;
			this.value = value;
			this.dropped = dropped;
		}

		/**
		 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
		 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
		 */
		public int estimateSize() {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE
				// version
				+ MemoryMeasuringConstants.INT_SIZE +
				// dropped
				+ MemoryMeasuringConstants.BYTE_SIZE +
				// key
				+ key.estimateSize()
				// value size estimate
				+ MemoryMeasuringConstants.REFERENCE_SIZE + (value == null ? 0 : EvitaDataTypes.estimateSize(value));
		}

		/**
		 * Returns true if this associatedData differs in key factors from the passed associatedData.
		 */
		@Override
		public boolean differsFrom(@Nullable AssociatedDataValue otherAssociatedDataValue) {
			if (otherAssociatedDataValue == null) return true;
			if (!Objects.equals(key, otherAssociatedDataValue.key)) return true;
			if (QueryUtils.valueDiffers(value, otherAssociatedDataValue.value)) return true;
			return dropped != otherAssociatedDataValue.dropped;
		}

		@Override
		public String toString() {
			return (dropped ? "❌" : "") +
				"\uD83D\uDD11 " + key.getAssociatedDataName() + " " +
				(key.getLocale() == null ? "" : "(" + key.getLocale() + ")") +
				": " +
				(
					value instanceof Object[] ?
						("[" + Arrays.stream((Object[]) value).filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(",")) + "]") :
						value == null ? "NULL" : value
				);
		}
	}
}
