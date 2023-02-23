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

package io.evitadb.api.io;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import io.evitadb.api.EntityCollectionBase;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.dataType.PaginatedList;
import io.evitadb.api.dataType.StripList;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.head.Entities;
import io.evitadb.api.query.require.*;
import io.evitadb.api.query.visitor.FinderVisitor;
import io.evitadb.api.utils.Assert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Evita request serves as simple DTO that streamlines and caches access to the input {@link Query}.
 *
 * **Note:** This class is abstract because it is expected, that implementations will add more methods like
 * {@link #getPrimaryKeys()}. In the end the abstract class will be merged with the final request implementation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public abstract class EvitaRequestBase {
	public static final int[] EMPTY_INTS = new int[0];
	@Getter private final Query query;
	@Getter private final ZonedDateTime alignedNow;
	private int[] primaryKeys;
	private boolean languageExamined;
	private Locale language;
	private Boolean requiredLanguages;
	private Set<Locale> requiredLanguagesSet;
	private QueryPriceMode queryPriceMode;
	private Boolean priceValidInTimeSet;
	private ZonedDateTime priceValidInTime;
	private Boolean entityBody;
	private Boolean entityAttributes;
	private Boolean entityAssociatedData;
	private Set<String> entityAssociatedDataSet;
	private Boolean entityReference;
	private Set<Serializable> entityReferenceSet;
	private PriceFetchMode entityPrices;
	private Boolean currencySet;
	private Currency currency;
	private Boolean requiresPriceLists;
	private Serializable[] priceLists;
	private Integer firstRecordOffset;
	private WithinHierarchy withinHierarchy;
	private Boolean requiredWithinHierarchy;
	private Integer limit;
	private ResultForm resultForm;
	private Map<Serializable, IntSet> facetGroupConjunction;
	private Map<Serializable, IntSet> facetGroupDisjunction;
	private Map<Serializable, IntSet> facetGroupNegation;
	private EntityContentRequire[] contentRequirements;

	/**
	 * Returns type of the entity this query targets. Allows to choose proper {@link EntityCollectionBase}.
	 */
	public Serializable getEntityType() {
		final Entities header = query.getEntities();
		Assert.notNull(header, "Entity is not specified in the query!");
		return header.getEntityType();
	}

	/**
	 * Returns language of the entity that is being requested.
	 */
	@Nullable
	public Locale getLanguage() {
		if (!this.languageExamined) {
			this.languageExamined = true;
			this.language = ofNullable(QueryUtils.findFilter(query, Language.class))
				.map(Language::getLanguage)
				.orElse(null);
		}
		return this.language;
	}

	/**
	 * Returns set of languages if requirement {@link DataInLanguage} is present in the query. If not it falls back to
	 * {@link Language} (check {@link DataInLanguage} docs).
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	@Nullable
	public Set<Locale> getRequiredLanguages() {
		if (this.requiredLanguages == null) {
			final DataInLanguage dataRequirement = QueryUtils.findRequire(query, DataInLanguage.class);
			if (dataRequirement != null) {
				this.requiredLanguagesSet = Arrays.stream(dataRequirement.getLanguages())
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			} else {
				final Locale theLanguage = getLanguage();
				if (theLanguage != null) {
					this.requiredLanguagesSet = Set.of(theLanguage);
				}
			}
			this.requiredLanguages = true;
		}
		return this.requiredLanguagesSet;
	}

	/**
	 * Returns query price mode of the current query.
	 */
	@Nonnull
	public QueryPriceMode getQueryPriceMode() {
		if (this.queryPriceMode == null) {
			this.queryPriceMode = ofNullable(QueryUtils.findRequire(query, UseOfPrice.class))
				.map(UseOfPrice::getQueryPriceMode)
				.orElse(QueryPriceMode.WITH_VAT);
		}
		return this.queryPriceMode;
	}

	/**
	 * Returns set of primary keys that are required by the query in {@link PrimaryKey} constraint.
	 * If there is no such constraint empty array is returned in the result.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	@Nonnull
	public int[] getPrimaryKeys() {
		if (primaryKeys == null) {
			primaryKeys = ofNullable(QueryUtils.findFilter(query, PrimaryKey.class))
				.map(PrimaryKey::getPrimaryKeys)
				.orElse(EMPTY_INTS);
		}
		return primaryKeys;
	}

	/**
	 * Returns TRUE if requirement {@link EntityBody} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	public boolean isRequiresEntityBody() {
		if (entityBody == null) {
			entityBody = ofNullable(QueryUtils.findRequire(query, EntityBody.class, SeparateEntityContentRequireContainer.class)).isPresent();
		}
		return entityBody ||
			isRequiresEntityAttributes() ||
			isRequiresEntityAssociatedData() ||
			isRequiresEntityReferences() ||
			getRequiresEntityPrices() != PriceFetchMode.NONE;
	}

	/**
	 * Returns TRUE if requirement {@link Attributes} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	public boolean isRequiresEntityAttributes() {
		if (entityAttributes == null) {
			entityAttributes = ofNullable(QueryUtils.findRequire(query, Attributes.class, SeparateEntityContentRequireContainer.class)).isPresent();
		}
		return entityAttributes;
	}

	/**
	 * Returns TRUE if requirement {@link Attributes} is present in the passed container constraint.
	 */
	public boolean isRequiresEntityAttributes(SeparateEntityContentRequireContainer insideContainer) {
		return ofNullable(QueryUtils.findRequire(insideContainer, Attributes.class)).isPresent();
	}

	/**
	 * Returns TRUE if requirement {@link AssociatedData} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	public boolean isRequiresEntityAssociatedData() {
		if (entityAssociatedData == null) {
			final AssociatedData requiresAssociatedData = QueryUtils.findRequire(query, AssociatedData.class, SeparateEntityContentRequireContainer.class);
			this.entityAssociatedData = requiresAssociatedData != null;
			this.entityAssociatedDataSet = requiresAssociatedData != null ?
				Arrays.stream(requiresAssociatedData.getAssociatedDataNames()).collect(Collectors.toSet()) :
				Collections.emptySet();
		}
		return entityAssociatedData;
	}

	/**
	 * Returns TRUE if requirement {@link AssociatedData} is present in the passed container constraint.
	 */
	public boolean isRequiresEntityAssociatedData(SeparateEntityContentRequireContainer insideContainer) {
		return ofNullable(QueryUtils.findRequire(insideContainer, AssociatedData.class)).isPresent();
	}

	/**
	 * Returns set of associated data names that were requested in the query. The set is empty if none is requested
	 * which means - all associated data is ought to be returned.
	 */
	@Nonnull
	public Set<String> getEntityAssociatedDataSet() {
		if (this.entityAssociatedDataSet == null) {
			isRequiresEntityAssociatedData();
		}
		return this.entityAssociatedDataSet;
	}

	/**
	 * Returns set of associated data names that were requested in the passed container constraint. The set is empty
	 * if none is requested which means - all associated data is ought to be returned.
	 */
	@Nonnull
	public Set<String> getEntityAssociatedDataSet(SeparateEntityContentRequireContainer insideContainer) {
		final AssociatedData requiresAssociatedData = QueryUtils.findRequire(insideContainer, AssociatedData.class);
		return requiresAssociatedData != null ?
			Arrays.stream(requiresAssociatedData.getAssociatedDataNames()).collect(Collectors.toSet()) :
			Collections.emptySet();
	}

	/**
	 * Returns TRUE if requirement {@link io.evitadb.api.query.require.References} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	public boolean isRequiresEntityReferences() {
		if (entityReference == null) {
			final References requiresReference = QueryUtils.findRequire(query, References.class, SeparateEntityContentRequireContainer.class);
			this.entityReference = requiresReference != null;
			this.entityReferenceSet = requiresReference != null ?
				Arrays.stream(requiresReference.getReferencedEntityType()).collect(Collectors.toSet()) :
				Collections.emptySet();
		}
		return entityReference;
	}

	/**
	 * Returns TRUE if requirement {@link io.evitadb.api.query.require.References} is present in the passed container constraint.
	 */
	public boolean isRequiresEntityReferences(SeparateEntityContentRequireContainer insideContainer) {
		return ofNullable(QueryUtils.findRequire(insideContainer, References.class)).isPresent();
	}

	/**
	 * Returns set of associated data names that were requested in the query. The set is empty if none is requested
	 * which means - all references ought to be returned.
	 */
	@Nonnull
	public Set<Serializable> getEntityReferenceSet() {
		if (this.entityReferenceSet == null) {
			isRequiresEntityReferences();
		}
		return this.entityReferenceSet;
	}

	/**
	 * Returns set of associated data names that were requested in the passed container constraint. The set is empty if
	 * none is requested which means - all associated data is ought to be returned.
	 */
	@Nullable
	public Set<Serializable> getEntityReferenceSet(SeparateEntityContentRequireContainer insideContainer) {
		final References requiresReference = QueryUtils.findRequire(insideContainer, References.class);
		return requiresReference != null ?
			Arrays.stream(requiresReference.getReferencedEntityType()).collect(Collectors.toSet()) :
			null;
	}

	/**
	 * Returns {@link PriceFetchMode} if requirement {@link Prices} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	@Nonnull
	public PriceFetchMode getRequiresEntityPrices() {
		if (this.entityPrices == null) {
			this.entityPrices = ofNullable(QueryUtils.findRequire(query, Prices.class, SeparateEntityContentRequireContainer.class))
				.map(Prices::getFetchMode)
				.orElse(PriceFetchMode.NONE);
		}
		return this.entityPrices;
	}

	/**
	 * Returns {@link PriceFetchMode} if requirement {@link Prices} is present in the passed container constraint.
	 */
	@Nonnull
	public PriceFetchMode getRequiresEntityPrices(SeparateEntityContentRequireContainer insideContainer) {
		return ofNullable(QueryUtils.findRequire(insideContainer, Prices.class))
			.map(Prices::getFetchMode)
			.orElse(PriceFetchMode.NONE);
	}

	/**
	 * Returns TRUE if any {@link PriceInPriceLists} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	public boolean isRequiresPriceLists() {
		if (this.requiresPriceLists == null) {
			final PriceInPriceLists pricesInPriceList = QueryUtils.findFilter(query, PriceInPriceLists.class);
			this.priceLists = ofNullable(pricesInPriceList)
				.map(PriceInPriceLists::getPriceLists)
				.orElse(new Serializable[0]);
			this.requiresPriceLists = pricesInPriceList != null;
		}
		return this.requiresPriceLists;
	}

	/**
	 * Returns set of price list ids if requirement {@link PriceInPriceLists} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	@Nonnull
	public Serializable[] getRequiresPriceLists() {
		if (this.priceLists == null) {
			isRequiresPriceLists();
		}
		return this.priceLists;
	}

	/**
	 * Returns set of price list ids if requirement {@link PriceInCurrency} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	@Nullable
	public Currency getRequiresCurrency() {
		if (this.currencySet == null) {
			this.currency = ofNullable(QueryUtils.findFilter(query, PriceInCurrency.class))
				.map(PriceInCurrency::getCurrency)
				.orElse(null);
			this.currencySet = true;
		}
		return this.currency;
	}

	/**
	 * Returns price valid in datetime if requirement {@link io.evitadb.api.query.filter.PriceValidIn} is present in the query.
	 * Accessor method cache the found result so that consecutive calls of this method are pretty fast.
	 */
	@Nullable
	public ZonedDateTime getRequiresPriceValidIn() {
		if (this.priceValidInTimeSet == null) {
			this.priceValidInTime = ofNullable(QueryUtils.findFilter(query, PriceValidIn.class))
				.map(PriceValidIn::getTheMoment)
				.orElse(null);
			this.priceValidInTimeSet = true;
		}
		return this.priceValidInTime;
	}

	/**
	 * Returns true if passed `groupId` of `entityType` facets are requested to be joined by conjunction (AND) instead
	 * of default disjunction (OR).
	 */
	public boolean isFacetGroupConjunction(@Nonnull Serializable entityType, int groupId) {
		if (this.facetGroupConjunction == null) {
			this.facetGroupConjunction = new HashMap<>();
			QueryUtils.findRequires(query, FacetGroupsConjunction.class)
				.forEach(it -> {
					final Serializable reqEntityType = it.getEntityType();
					final IntHashSet reqFacetGroups = new IntHashSet(it.getFacetGroups().length);
					reqFacetGroups.addAll(it.getFacetGroups());
					this.facetGroupConjunction.put(reqEntityType, reqFacetGroups);
				});
		}
		return ofNullable(this.facetGroupConjunction.get(entityType))
			.map(it -> it.contains(groupId))
			.orElse(false);
	}

	/**
	 * Returns true if passed `groupId` of `entityType` is requested to be joined with other facet groups by
	 * disjunction (OR) instead of default conjunction (AND).
	 */
	public boolean isFacetGroupDisjunction(@Nonnull Serializable entityType, Integer groupId) {
		if (this.facetGroupDisjunction == null) {
			this.facetGroupDisjunction = new HashMap<>();
			QueryUtils.findRequires(query, FacetGroupsDisjunction.class)
				.forEach(it -> {
					final Serializable reqEntityType = it.getEntityType();
					final IntHashSet reqFacetGroups = new IntHashSet(it.getFacetGroups().length);
					reqFacetGroups.addAll(it.getFacetGroups());
					this.facetGroupDisjunction.put(reqEntityType, reqFacetGroups);
				});
		}
		return ofNullable(this.facetGroupDisjunction.get(entityType))
			.map(it -> it.contains(groupId))
			.orElse(false);
	}

	/**
	 * Returns true if passed `groupId` of `entityType` facets are requested to be joined by negation (AND NOT) instead
	 * of default disjunction (OR).
	 */
	public boolean isFacetGroupNegation(@Nonnull Serializable entityType, Integer groupId) {
		if (this.facetGroupNegation == null) {
			this.facetGroupNegation = new HashMap<>();
			QueryUtils.findRequires(query, FacetGroupsNegation.class)
				.forEach(it -> {
					final Serializable reqEntityType = it.getEntityType();
					final IntHashSet reqFacetGroups = new IntHashSet(it.getFacetGroups().length);
					reqFacetGroups.addAll(it.getFacetGroups());
					this.facetGroupNegation.put(reqEntityType, reqFacetGroups);
				});
		}
		return ofNullable(this.facetGroupNegation.get(entityType))
			.map(it -> it.contains(groupId))
			.orElse(false);
	}

	/**
	 * Returns count of records required in the result (i.e. number of records on a single page).
	 */
	public int getLimit() {
		if (limit == null) {
			initPagination();
		}
		return limit;
	}

	/**
	 * Returns requested record offset of the records required in the result.
	 */
	public int getFirstRecordOffset() {
		if (firstRecordOffset == null) {
			initPagination();
		}
		return firstRecordOffset;
	}

	/**
	 * Returns requested record offset of the records required in the result.
	 * Offset is automatically reset to zero if requested offset exceeds the total available record count.
	 */
	public int getFirstRecordOffset(int totalRecordCount) {
		if (firstRecordOffset == null) {
			initPagination();
		}
		return firstRecordOffset >= totalRecordCount ? 0 : firstRecordOffset;
	}

	/**
	 * Returns {@link WithinHierarchy} constraint
	 */
	@Nullable
	public WithinHierarchy getWithinHierarchy(Serializable entityType) {
		if (requiredWithinHierarchy == null && query.getFilterBy() != null) {
			withinHierarchy = QueryUtils.findFilter(
				query.getFilterBy(),
				filterConstraint -> filterConstraint instanceof WithinHierarchy &&
					Objects.equals(((WithinHierarchy) filterConstraint).getEntityType(), entityType)
			);
			requiredWithinHierarchy = true;
		}
		return withinHierarchy;
	}

	/**
	 * Method creates requested implementation of {@link DataChunk} with results.
	 */
	@Nonnull
	public <T extends Serializable> DataChunk<T> createDataChunk(int totalRecordCount, List<T> data) {
		if (firstRecordOffset == null) {
			initPagination();
		}
		switch (resultForm) {
			case PAGINATED_LIST:
				return new PaginatedList<>(limit == 0 ? 1 : (firstRecordOffset + limit) / limit, limit, totalRecordCount, data);
			case STRIP_LIST:
				return new StripList<>(firstRecordOffset, limit, totalRecordCount, data);
			default:
				throw new IllegalStateException("Unknown result type!");
		}
	}

	/**
	 * Method will find all constraints implementing {@link EntityContentRequire} interface (i.e. determine the richness
	 * of the returned entity) in input query. The constraints inside {@link ExtraResultRequireConstraint} implementing
	 * constraints are ignored because they relate to the different entity context.
	 */
	@Nonnull
	public EntityContentRequire[] getRequiredContents() {
		if (this.contentRequirements == null) {
			if (query.getRequire() == null) {
				this.contentRequirements = new EntityContentRequire[0];
			} else {
				this.contentRequirements = FinderVisitor.findConstraints(
						query.getRequire(), EntityContentRequire.class::isInstance, ExtraResultRequireConstraint.class::isInstance
					)
					.stream()
					.map(EntityContentRequire.class::cast)
					.toArray(EntityContentRequire[]::new);
			}
		}
		return this.contentRequirements;
	}

	/**
	 * Internal method that consults input query and initializes pagination information.
	 * If there is no pagination in the input query, first page with size of 20 records is used as default.
	 */
	private void initPagination() {
		final Optional<Page> page = ofNullable(QueryUtils.findRequire(query, Page.class));
		final Optional<Strip> strip = ofNullable(QueryUtils.findRequire(query, Strip.class));
		if (page.isPresent()) {
			limit = page.get().getPageSize();
			firstRecordOffset = PaginatedList.getFirstItemNumberForPage(page.get().getPageNumber(), limit);
			resultForm = ResultForm.PAGINATED_LIST;
		} else if (strip.isPresent()) {
			limit = strip.get().getLimit();
			firstRecordOffset = strip.get().getOffset();
			resultForm = ResultForm.STRIP_LIST;
		} else {
			limit = 20;
			firstRecordOffset = 0;
			resultForm = ResultForm.PAGINATED_LIST;
		}
	}

	private enum ResultForm {
		PAGINATED_LIST, STRIP_LIST
	}

}
