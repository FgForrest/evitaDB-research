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

import io.evitadb.api.SqlEvita;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.QueryUtils;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.api.query.filter.WithinRootHierarchy;
import io.evitadb.api.query.order.PriceAscending;
import io.evitadb.api.query.order.PriceDescending;
import io.evitadb.api.query.require.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Implementation of {@link EvitaRequestBase} for PostgreSQL implementation of {@link SqlEvita}.
 *
 * @author Lukáš Hornych 2021
 */
public class SqlEvitaRequest extends EvitaRequestBase {

    private Boolean requiresPrices;
    private Boolean requiresPriceOrdering;

    private Boolean requiresFacetSummary;

    private Boolean requiresAttributeHistogram;
    private String[] attributeHistogramsNames;
    private Integer attributeHistogramBuckets;

    private Boolean requiresPriceHistogram;
    private Integer priceHistogramBuckets;

    private Boolean requiresParents;
    private Parents requiredParents;
    private List<ParentsOfType> requiredParentsOfType;

    private Serializable withinHierarchyEntityType;

    private Boolean requiresHierarchyStatistics;
    private List<HierarchyStatistics> requiredHierarchyStatistics;

    private Map<Serializable, List<Integer>> selectedFacets;
    private FacetStatisticsDepth facetStatisticsDepth;

    private QueryPriceMode queryPriceMode;

    public SqlEvitaRequest(Query query, ZonedDateTime alignedNow) {
        super(query, alignedNow);
    }

    /**
     * Returns true if prices for filtering are needed by query
     */
    public boolean isRequiresPrices() {
        if (this.requiresPrices == null) {
            final Serializable[] priceLists = getRequiresPriceLists();
            final Currency currency = getRequiresCurrency();
            this.requiresPrices = (priceLists.length != 0) || (currency != null);
        }
        return this.requiresPrices;
    }

    /**
     * Returns true if prices for ordering are needed by query
     */
    public boolean isRequiresPriceOrdering() {
        if (this.requiresPriceOrdering == null) {
            if (!isRequiresPrices()) {
                this.requiresPriceOrdering = false;
            } else {
                if ((QueryUtils.findOrder(getQuery(), PriceAscending.class) != null) ||
                    (QueryUtils.findOrder(getQuery(), PriceDescending.class) != null)) {
                    this.requiresPriceOrdering = true;
                } else {
                    this.requiresPriceOrdering = false;
                }
            }
        }
        return this.requiresPriceOrdering;
    }

    /**
     * Returns true if {@link FacetSummary} is present in query.
     */
    public boolean isRequiresFacetSummary() {
        if (requiresFacetSummary == null) {
            this.requiresFacetSummary = ofNullable(QueryUtils.findRequire(getQuery(), FacetSummary.class)).isPresent();
        }
        return requiresFacetSummary;
    }

    /**
     * Returns true if {@link AttributeHistogram} is present in query.
     */
    public boolean isRequiresAttributeHistogram() {
        if (requiresAttributeHistogram == null) {
            final AttributeHistogram attributeHistogram = QueryUtils.findRequire(getQuery(), AttributeHistogram.class);
            if (attributeHistogram == null) {
                this.requiresAttributeHistogram = false;
                this.attributeHistogramsNames = new String[0];
                this.attributeHistogramBuckets = 0;
            } else {
                this.requiresAttributeHistogram = true;
                this.attributeHistogramsNames = attributeHistogram.getAttributeNames();
                this.attributeHistogramBuckets = attributeHistogram.getRequestedBucketCount();
            }
        }
        return requiresAttributeHistogram;
    }

    /**
     * Returns true if {@link PriceHistogram} is present in query.
     */
    public boolean isRequiresPriceHistogram() {
        if (requiresPriceHistogram == null) {
            final PriceHistogram priceHistogram = QueryUtils.findRequire(getQuery(), PriceHistogram.class);
            if (priceHistogram == null) {
                this.requiresPriceHistogram = false;
                this.priceHistogramBuckets = 0;
            } else {
                this.requiresPriceHistogram = true;
                this.priceHistogramBuckets = priceHistogram.getRequestedBucketCount();
            }
        }
        return requiresPriceHistogram;
    }

    /**
     * Returns names of attributes for required histograms
     */
    public String[] getRequiresAttributeHistogramsNames() {
        if (attributeHistogramsNames == null) {
            isRequiresAttributeHistogram();
        }
        return attributeHistogramsNames;
    }

    /**
     * Returns required number of buckets in each histogram
     */
    public int getRequiresAttributeHistogramBuckets() {
        if (attributeHistogramBuckets == null) {
            isRequiresAttributeHistogram();
        }
        return attributeHistogramBuckets;
    }

    /**
     * Returns required number of buckets in each histogram
     */
    public int getRequiresPriceHistogramBuckets() {
        if (priceHistogramBuckets == null) {
            isRequiresPriceHistogram();
        }
        return priceHistogramBuckets;
    }

    /**
     * Returns true if {@link Parents} is present in query.
     */
    public boolean isRequiresParents() {
        if (this.requiresParents == null) {
            final Parents parents = QueryUtils.findRequire(getQuery(), Parents.class);
            if (parents != null) {
                // all parents are wanted
                this.requiresParents = true;
                this.requiredParents = parents;
                this.requiredParentsOfType = List.of();
            } else {
                final List<ParentsOfType> parentsOfTypeList = QueryUtils.findRequires(getQuery(), ParentsOfType.class);
                if (!parentsOfTypeList.isEmpty()) {
                    // only selected parents are wanted
                    this.requiresParents = true;
                    this.requiredParentsOfType = parentsOfTypeList;
                } else {
                    // no parents are wanted
                    this.requiresParents = false;
                    this.requiredParentsOfType = List.of();
                }
            }
        }
        return this.requiresParents;
    }

    /**
     * Returns {@link Parents} present in query
     */
    @Nullable
    public Parents getRequiredParents() {
        if (this.requiresParents == null) {
            isRequiresParents();
        }
        return this.requiredParents;
    }

    /**
     * Returns list of all {@link ParentsOfType} present in query.
     */
    @Nonnull
    public List<ParentsOfType> getRequiredParentsOfType() {
        if (this.requiresParents == null) {
            isRequiresParents();
        }
        return this.requiredParentsOfType;
    }

    /**
     * Returns entity type of hierarchy collection used in either {@link WithinHierarchy} or {@link WithinRootHierarchy}
     */
    public Serializable getWithinHierarchyEntityType() {
        if (this.withinHierarchyEntityType == null) {
            final WithinHierarchy withinHierarchy = QueryUtils.findFilter(getQuery(), WithinHierarchy.class);
            if (withinHierarchy != null) {
                this.withinHierarchyEntityType = withinHierarchy.getEntityType();
            } else {
                final WithinRootHierarchy withinRootHierarchy = QueryUtils.findFilter(getQuery(), WithinRootHierarchy.class);
                if (withinRootHierarchy != null) {
                    this.withinHierarchyEntityType = withinRootHierarchy.getEntityType();
                }
            }
        }
        return this.withinHierarchyEntityType;
    }

    /**
     * Returns true if at least single {@link HierarchyStatistics} is present in query.
     */
    public boolean isRequiresHierarchyStatistics() {
        if (this.requiresHierarchyStatistics == null) {
            final List<HierarchyStatistics> hierarchyStatisticsList = QueryUtils.findRequires(getQuery(), HierarchyStatistics.class);
            if (hierarchyStatisticsList.isEmpty()) {
                this.requiresHierarchyStatistics = false;
                this.requiredHierarchyStatistics = List.of();
            } else {
                this.requiresHierarchyStatistics = true;
                this.requiredHierarchyStatistics = hierarchyStatisticsList;
            }
        }
        return this.requiresHierarchyStatistics;
    }

    /**
     * Returns list of all {@link HierarchyStatistics} present in query
     */
    @Nonnull
    public List<HierarchyStatistics> getRequiredHierarchyStatistics() {
        if (this.requiresHierarchyStatistics == null) {
            isRequiresHierarchyStatistics();
        }
        return this.requiredHierarchyStatistics;
    }

    /**
     * Returns all ids of {@link Facet}s found in query grouped by their entity types.
     */
    public Map<Serializable, List<Integer>> getSelectedFacets() {
        if (selectedFacets == null) {
            final List<Facet> facets = QueryUtils.findFilters(getQuery(), Facet.class);

            final Map<Serializable, List<Integer>> groupedSelectedFacets = new HashMap<>();
            facets.forEach(facet -> {
                final List<Integer> facetIdsOfGroup = groupedSelectedFacets.computeIfAbsent(facet.getEntityType(), k -> new LinkedList<>());
                facetIdsOfGroup.addAll(Arrays.stream(facet.getFacetIds()).boxed().collect(Collectors.toList()));
            });

            this.selectedFacets = groupedSelectedFacets;
        }
        return selectedFacets;
    }

    /**
     * Returns facet statistics depth set by query
     */
    public FacetStatisticsDepth getFacetStatisticsDepth() {
        if (facetStatisticsDepth == null) {
            final FacetSummary facetSummary = QueryUtils.findRequire(getQuery(), FacetSummary.class);
            facetStatisticsDepth = facetSummary.getFacetStatisticsDepth();
        }
        return facetStatisticsDepth;
    }

    public boolean isRequiresAnyExtraResult() {
        return this.isRequiresFacetSummary()
                || this.isRequiresAttributeHistogram()
                || this.isRequiresPriceHistogram()
                || this.isRequiresParents()
                || this.isRequiresHierarchyStatistics();
    }

    public QueryPriceMode getRequiredPriceMode() {
        if (this.queryPriceMode == null) {
            this.queryPriceMode = ofNullable(QueryUtils.findRequire(this.getQuery(), UseOfPrice.class))
                    .map(UseOfPrice::getQueryPriceMode)
                    .orElse(QueryPriceMode.WITH_VAT);
        }
        return queryPriceMode;
    }
}
