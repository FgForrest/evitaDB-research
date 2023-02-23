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

import io.evitadb.api.query.Query;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.api.query.head.Entities;
import io.evitadb.api.query.require.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static io.evitadb.api.query.QueryConstraints.language;
import static io.evitadb.api.query.QueryUtils.findFilter;
import static io.evitadb.api.query.QueryUtils.findRequire;

/**
 * {@link EvitaRequest} is internal class (Evita accepts simple {@link Query} object - see {@link EvitaSession#query(Query, Class)})
 * that envelopes the input query. Evita request can be used to implement methods that extract crucial informations
 * from the input query and cache those extracted information to avoid paying parsing costs twice in single request.
 * See examples in {@link EvitaResponseBase} super class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Getter
@Setter
public class EsEvitaRequest extends EvitaRequestBase {

    private FacetSummary facetSummary;
    private Boolean requiresAttributeHistogram;
    private String[] attributeHistogramsNames;
    private Integer attributeHistogramBuckets;
    private Double priceHistogramInterval;
    private Double priceHistogramMax;
    private Double priceHistogramMin;
    private Map<String, Double> attributeHistogramIntervals = new HashMap<>();
    private Map<String, Double> attributeHistogramMax = new HashMap<>();
    private Map<String, Double> attributeHistogramMin = new HashMap<>();
    private Boolean requiresPriceHistogram;
    private Integer priceHistogramBuckets;
    private FacetStatisticsDepth facetStatisticsDepth;
    private QueryPriceMode queryPriceMode;
    private Parents parents;
    private WithinHierarchy withinHierarchy;
    private HierarchyStatistics hierarchyStatistics;
    private ParentsOfType parentsOfType;

    public EsEvitaRequest(Query query, ZonedDateTime alignedNow) {
        super(query, alignedNow);
    }

    public EsEvitaRequest(Serializable entityType, Locale language, EntityContentRequire[] requirements, ZonedDateTime alignedNow) {
        super(Query.query(new Entities(entityType),language == null ? null :new FilterBy(language(language)), new Require(requirements)), alignedNow);
    }

    /**
     * Returns {@link FacetSummary} if present in query.
     */
    public FacetSummary getFacetSummary() {
        if (facetSummary == null) {
            this.facetSummary = findRequire(getQuery(), FacetSummary.class);
        }
        return facetSummary;
    }

    /**
     * Returns true if {@link AttributeHistogram} is present in query.
     */
    public boolean isRequiresAttributeHistogram() {
        if (requiresAttributeHistogram == null) {
            final AttributeHistogram attributeHistogram = findRequire(getQuery(), AttributeHistogram.class);
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
            final PriceHistogram priceHistogram = findRequire(getQuery(), PriceHistogram.class);
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

    public WithinHierarchy getWithinHierarchyFilter() {
        if (this.withinHierarchy == null) {
            this.withinHierarchy = findFilter(this.getQuery(), WithinHierarchy.class);
        }
        return withinHierarchy;
    }

    public HierarchyStatistics getRequireHierarchyStatistics() {
        if (this.hierarchyStatistics == null) {
            this.hierarchyStatistics = findRequire(this.getQuery(), HierarchyStatistics.class);
        }
        return hierarchyStatistics;
    }

    public Parents getRequireParents() {
        if (this.parents == null) {
            this.parents = findRequire(this.getQuery(), Parents.class);
        }
        return parents;
    }

    public ParentsOfType getRequireParentsOfType() {
        if (this.parentsOfType == null) {
            this.parentsOfType = findRequire(this.getQuery(), ParentsOfType.class);
        }
        return parentsOfType;
    }
}
