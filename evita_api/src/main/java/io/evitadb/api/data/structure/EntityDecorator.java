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

import io.evitadb.api.data.*;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.mutation.EntityMutation;
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.structure.ExistingEntityBuilder.ExistingEntityMutation;
import io.evitadb.api.exception.ContextMissingException;
import io.evitadb.api.io.predicate.*;
import io.evitadb.api.query.require.QueryPriceMode;
import io.evitadb.api.schema.AssociatedDataSchema;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Entity decorator class envelopes any {@link Entity} and allows to filter out properties that are not passing predicate
 * conditions. This allows us to reuse rich {@link Entity} objects from the cache even if clients requests thinner ones.
 * For example if we have full-blown entity in our cache and client asks for entity in English language, we can use
 * entity decorator to hide all attributes that refers to other languages than English one.
 *
 * We try to keep Evita DB responses consistent and provide only those type of data that were really requested in the query
 * and avoid inconsistent situations that richer data are returned just because the entity was found in cache in a form
 * that more than fulfills the request.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EntityDecorator implements SealedEntity {
	private static final long serialVersionUID = -3641248774594311898L;
	/**
	 * Contains reference to the (possibly richer than requested) entity object.
	 */
	@Getter private final Entity delegate;
	/**
	 * Contains actual entity schema.
	 */
	private final EntitySchema entitySchema;
	/**
	 * This predicate filters out invalid hierarchy placements.
	 */
	@Getter private final HierarchicalContractSerializablePredicate hierarchicalPlacementPredicate;
	/**
	 * This predicate filters out attributes that were not fetched in query.
	 */
	@Getter private final AttributeValueSerializablePredicate attributePredicate;
	/**
	 * This predicate filters out associated data that were not fetched in query.
	 */
	@Getter private final AssociatedDataValueSerializablePredicate associatedDataPredicate;
	/**
	 * This predicate filters out references that were not fetched in query.
	 */
	@Getter private final ReferenceContractSerializablePredicate referencePredicate;
	/**
	 * This predicate filters out prices that were not fetched in query.
	 */
	@Getter private final PriceContractSerializablePredicate pricePredicate;

	/**
	 * Reflection lookup is used for (de)serialization of {@link io.evitadb.api.dataType.ComplexDataObject}.
	 */
	transient ReflectionLookup reflectionLookup;
	/**
	 * Optimization that ensures that expensive attributes filtering using predicates happens only once.
	 */
	private List<AttributeValue> filteredAttributes;
	/**
	 * Optimization that ensures that expensive associated data filtering using predicates happens only once.
	 */
	private List<AssociatedDataValue> filteredAssociatedData;
	/**
	 * Optimization that ensures that expensive reference filtering using predicates happens only once.
	 */
	private List<ReferenceContract> filteredReferences;
	/**
	 * Optimization that ensures that expensive prices filtering using predicates happens only once.
	 */
	private List<PriceContract> filteredPrices;

	/**
	 * Creates wrapper around {@link Entity} that filters existing data according passed predicates (which are constructed
	 * to match query that is used to retrieve the decorator).
	 *
	 * @param delegate                       fully or partially loaded entity - it's usually wider than decorator (may be even complete), delegate
	 *                                       might be obtained from shared global cache
	 * @param entitySchema                   schema of the delegate entity
	 * @param hierarchicalPlacementPredicate predicate used to filter out hierarchy placement to match input query
	 * @param attributePredicate             predicate used to filter out attributes to match input query
	 * @param associatedDataPredicate        predicate used to filter out associated data to match input query
	 * @param referencePredicate             predicate used to filter out references to match input query
	 * @param pricePredicate                 predicate used to filter out prices to match input query
	 */
	public EntityDecorator(
		@Nonnull Entity delegate,
		@Nonnull EntitySchema entitySchema,
		@Nonnull HierarchicalContractSerializablePredicate hierarchicalPlacementPredicate,
		@Nonnull AttributeValueSerializablePredicate attributePredicate,
		@Nonnull AssociatedDataValueSerializablePredicate associatedDataPredicate,
		@Nonnull ReferenceContractSerializablePredicate referencePredicate,
		@Nonnull PriceContractSerializablePredicate pricePredicate
	) {
		this.delegate = delegate;
		this.entitySchema = entitySchema;
		this.hierarchicalPlacementPredicate = hierarchicalPlacementPredicate;
		this.attributePredicate = attributePredicate;
		this.associatedDataPredicate = associatedDataPredicate;
		this.referencePredicate = referencePredicate;
		this.pricePredicate = pricePredicate;
	}

	@Nonnull
	@Override
	public Serializable getType() {
		return delegate.getType();
	}

	@Nonnull
	@Override
	public EntitySchema getSchema() {
		return entitySchema;
	}

	@Nullable
	@Override
	public Integer getPrimaryKey() {
		return delegate.getPrimaryKey();
	}

	@Nullable
	@Override
	public HierarchicalPlacementContract getHierarchicalPlacement() {
		return ofNullable(delegate.getHierarchicalPlacement())
			.filter(hierarchicalPlacementPredicate)
			.orElse(null);
	}

	@Nonnull
	@Override
	public Collection<ReferenceContract> getReferences() {
		if (filteredReferences == null) {
			filteredReferences = delegate.getReferences()
				.stream()
				.filter(referencePredicate)
				.map(it -> new ReferenceDecorator(it, attributePredicate))
				.collect(Collectors.toList());
		}
		return filteredReferences;
	}

	@Nonnull
	@Override
	public Collection<ReferenceContract> getReferences(Serializable referencedEntityType) {
		return getReferences()
			.stream()
			.filter(it -> Objects.equals(referencedEntityType, it.getReferencedEntity().getType()))
			.collect(Collectors.toList());
	}

	@Nullable
	@Override
	public ReferenceContract getReference(Serializable referencedEntityType, int referencedEntityId) {
		return ofNullable(delegate.getReference(referencedEntityType, referencedEntityId))
			.filter(referencePredicate)
			.map(it -> new ReferenceDecorator(it, attributePredicate))
			.orElse(null);
	}

	@Nonnull
	@Override
	public Set<Locale> getLocales() {
		return delegate.getLocales();
	}

	@Override
	public boolean isDropped() {
		return delegate.isDropped();
	}

	@Override
	public int getVersion() {
		return delegate.getVersion();
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName) {
		//noinspection unchecked
		return ofNullable(getAttributeValue(attributeName))
			.map(it -> (T) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName) {
		//noinspection unchecked
		return ofNullable(getAttributeValue(attributeName))
			.map(it -> (T[]) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName) {
		final Set<Locale> requestedLocales = attributePredicate.getLanguages();
		if (requestedLocales == null) {
			return ofNullable(delegate.getAttributeValue(attributeName))
				.filter(attributePredicate)
				.orElse(null);
		} else {
			AttributeValue result = delegate.getAttributeValue(attributeName);
			if (result == null) {
				Locale resultLocale = null;
				final Set<Locale> examinedLocales = requestedLocales.isEmpty() ? delegate.getAttributeLocales() : requestedLocales;
				for (Locale requestedLocale : examinedLocales) {
					final AttributeValue resultAdept = delegate.getAttributeValue(attributeName, requestedLocale);
					if (result == null) {
						result = resultAdept;
						resultLocale = requestedLocale;
					} else {
						throw new IllegalArgumentException("Attribute `" + attributeName + "` has multiple values for different locales: `" + resultLocale + "` and `" + requestedLocale + "`!");
					}
				}
			}
			return ofNullable(result)
				.filter(attributePredicate)
				.orElse(null);
		}
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T getAttribute(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return ofNullable(delegate.getAttributeValue(attributeName, locale))
			.filter(attributePredicate)
			.map(it -> (T) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable & Comparable<?>> T[] getAttributeArray(@Nonnull String attributeName, @Nonnull Locale locale) {
		//noinspection unchecked
		return ofNullable(delegate.getAttributeValue(attributeName, locale))
			.filter(attributePredicate)
			.map(it -> (T[]) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeValue getAttributeValue(@Nonnull String attributeName, @Nonnull Locale locale) {
		return ofNullable(delegate.getAttributeValue(attributeName, locale))
			.filter(attributePredicate)
			.orElse(null);
	}

	@Nullable
	@Override
	public AttributeSchema getAttributeSchema(@Nonnull String attributeName) {
		return delegate.getAttributeSchema(attributeName);
	}

	@Nonnull
	@Override
	public Set<String> getAttributeNames() {
		return getAttributeValues()
			.stream()
			.map(it -> it.getKey().getAttributeName())
			.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Set<AttributeKey> getAttributeKeys() {
		return getAttributeValues()
			.stream()
			.map(AttributeValue::getKey)
			.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Collection<AttributeValue> getAttributeValues() {
		if (filteredAttributes == null) {
			filteredAttributes = delegate.getAttributeValues()
				.stream()
				.filter(attributePredicate)
				.collect(Collectors.toList());
		}
		return filteredAttributes;
	}

	@Nonnull
	@Override
	public Collection<AttributeValue> getAttributeValues(@Nonnull String attributeName) {
		return getAttributeValues()
			.stream()
			.filter(it -> attributeName.equals(it.getKey().getAttributeName()))
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Set<Locale> getAttributeLocales() {
		return this.delegate.getAttributeLocales();
	}

	@Nonnull
	public Set<Locale> getFetchedAttributeLocales() {
		return getAttributeValues()
			.stream()
			.map(it -> it.getKey().getLocale())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName) {
		//noinspection unchecked
		return ofNullable(getAssociatedDataValue(associatedDataName))
			.map(it -> (T) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, Class<T> dtoType) {
		return ofNullable(getAssociatedDataValue(associatedDataName))
			.map(it -> DataObjectConverter.getOriginalForm(it.getValue(), dtoType, getReflectionLookup()))
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName) {
		//noinspection unchecked
		return ofNullable(getAssociatedDataValue(associatedDataName))
			.map(it -> (T[]) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName) {
		final Set<Locale> requestedLocales = associatedDataPredicate.getLanguages();
		if (requestedLocales == null) {
			return ofNullable(delegate.getAssociatedDataValue(associatedDataName))
				.filter(associatedDataPredicate)
				.orElse(null);
		} else {
			AssociatedDataValue result = delegate.getAssociatedDataValue(associatedDataName);
			if (result == null) {
				Locale resultLocale = null;
				final Set<Locale> examinedLocales = requestedLocales.isEmpty() ? delegate.getAssociatedDataLocales() : requestedLocales;
				for (Locale requestedLocale : examinedLocales) {
					final AssociatedDataValue resultAdept = delegate.getAssociatedDataValue(associatedDataName, requestedLocale);
					if (result == null) {
						result = resultAdept;
						resultLocale = requestedLocale;
					} else {
						throw new IllegalArgumentException("Associated data `" + associatedDataName + "` has multiple values for different locales: `" + resultLocale + "` and `" + requestedLocale + "`!");
					}
				}
			}
			return ofNullable(result)
				.filter(associatedDataPredicate)
				.orElse(null);
		}
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return ofNullable(delegate.getAssociatedDataValue(associatedDataName, locale))
			.filter(associatedDataPredicate)
			.map(it -> (T) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T getAssociatedData(@Nonnull String associatedDataName, @Nonnull Locale locale, Class<T> dtoType) {
		return ofNullable(delegate.getAssociatedDataValue(associatedDataName, locale))
			.filter(associatedDataPredicate)
			.map(AssociatedDataValue::getValue)
			.map(it -> DataObjectConverter.getOriginalForm(it, dtoType, getReflectionLookup()))
			.orElse(null);
	}

	@Nullable
	@Override
	public <T extends Serializable> T[] getAssociatedDataArray(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		//noinspection unchecked
		return ofNullable(delegate.getAssociatedDataValue(associatedDataName, locale))
			.filter(associatedDataPredicate)
			.map(it -> (T[]) it.getValue())
			.orElse(null);
	}

	@Nullable
	@Override
	public AssociatedDataValue getAssociatedDataValue(@Nonnull String associatedDataName, @Nonnull Locale locale) {
		return ofNullable(delegate.getAssociatedDataValue(associatedDataName, locale))
			.filter(associatedDataPredicate)
			.orElse(null);
	}

	@Nullable
	@Override
	public AssociatedDataSchema getAssociatedDataSchema(@Nonnull String associatedDataName) {
		return delegate.getAssociatedDataSchema(associatedDataName);
	}

	@Nonnull
	@Override
	public Set<String> getAssociatedDataNames() {
		return getAssociatedDataValues()
			.stream()
			.map(it -> it.getKey().getAssociatedDataName())
			.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Set<AssociatedDataKey> getAssociatedDataKeys() {
		return getAssociatedDataValues()
			.stream()
			.map(AssociatedDataValue::getKey)
			.collect(Collectors.toSet());
	}

	@Nonnull
	@Override
	public Collection<AssociatedDataValue> getAssociatedDataValues() {
		if (filteredAssociatedData == null) {
			filteredAssociatedData = delegate.getAssociatedDataValues()
				.stream()
				.filter(associatedDataPredicate)
				.collect(Collectors.toList());
		}
		return filteredAssociatedData;
	}

	@Nonnull
	@Override
	public Collection<AssociatedDataValue> getAssociatedDataValues(@Nonnull String associatedDataName) {
		return getAssociatedDataValues()
			.stream()
			.filter(it -> associatedDataName.equals(it.getKey().getAssociatedDataName()))
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Set<Locale> getAssociatedDataLocales() {
		return this.delegate.getAssociatedDataLocales();
	}

	@Nonnull
	public Set<Locale> getFetchedAssociatedDataLocales() {
		return getAssociatedDataValues()
			.stream()
			.map(it -> it.getKey().getLocale())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	@Override
	public PriceContract getPrice(int priceId, @Nonnull Serializable priceList, @Nonnull Currency currency) {
		return ofNullable(delegate.getPrice(priceId, priceList, currency))
			.filter(pricePredicate)
			.orElse(null);
	}

	@Nullable
	@Override
	public PriceContract getSellingPrice() throws ContextMissingException {
		if (!isContextAvailable()) {
			throw new ContextMissingException();
		}
		return getSellingPrice(
			pricePredicate.getCurrency(),
			pricePredicate.getValidIn(),
			pricePredicate.getPriceLists()
		);
	}

	@Nullable
	public PriceContract getSellingPrice(@Nonnull Predicate<PriceContract> predicate) throws ContextMissingException {
		if (!isContextAvailable()) {
			throw new ContextMissingException();
		}
		return PricesContract.computeSellingPrice(
			getPrices(),
			getPriceInnerRecordHandling(),
			pricePredicate.getCurrency(),
			pricePredicate.getValidIn(),
			pricePredicate.getPriceLists(),
			predicate
		);
	}

	@Nonnull
	@Override
	public List<PriceContract> getAllSellingPrices() {
		return getAllSellingPrices(
			pricePredicate.getCurrency(),
			pricePredicate.getValidIn(),
			pricePredicate.getPriceLists()
		);
	}

	@Override
	public boolean hasPriceInInterval(@Nonnull BigDecimal from, @Nonnull BigDecimal to, @Nonnull QueryPriceMode queryPriceMode) throws ContextMissingException {
		if (isContextAvailable()) {
			throw new ContextMissingException();
		}
		return hasPriceInInterval(
			from, to, queryPriceMode,
			pricePredicate.getCurrency(),
			pricePredicate.getValidIn(),
			pricePredicate.getPriceLists()
		);
	}

	@Nonnull
	@Override
	public Collection<PriceContract> getPrices() {
		if (filteredPrices == null) {
			filteredPrices = delegate.getPrices()
				.stream()
				.filter(pricePredicate)
				.collect(Collectors.toList());
		}
		return filteredPrices;
	}

	@Nonnull
	@Override
	public PriceInnerRecordHandling getPriceInnerRecordHandling() {
		return delegate.getPriceInnerRecordHandling();
	}

	@Override
	public int getPricesVersion() {
		return delegate.getPricesVersion();
	}

	@Nonnull
	@Override
	public EntityBuilder open() {
		return new ExistingEntityBuilder(this);
	}

	@Nonnull
	@Override
	public EntityMutation withMutations(LocalMutation<?, ?>... localMutations) {
		return new ExistingEntityMutation(delegate, localMutations);
	}

	@Nonnull
	@Override
	public EntityMutation withMutations(Collection<LocalMutation<?, ?>> localMutations) {
		return new ExistingEntityMutation(delegate, localMutations);
	}

	public boolean isContextAvailable() {
		return pricePredicate.getCurrency() != null && !ArrayUtils.isEmpty(pricePredicate.getPriceLists());
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj instanceof EntityDecorator ? ((EntityDecorator) obj).delegate : obj);
	}

	@Override
	public String toString() {
		return describe();
	}

	private ReflectionLookup getReflectionLookup() {
		return ofNullable(reflectionLookup).orElseGet(() -> {
			this.reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE);
			return this.reflectionLookup;
		});
	}

}
