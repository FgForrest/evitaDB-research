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

package io.evitadb.storage.query.builder.facet;

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.FilterBy;
import io.evitadb.api.query.require.FacetGroupsConjunction;
import io.evitadb.api.query.require.FacetGroupsDisjunction;
import io.evitadb.api.query.require.FacetGroupsNegation;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.storage.EntityCollectionContext;
import io.evitadb.storage.serialization.typedValue.StringTypedValueSerializer;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.evitadb.api.query.QueryUtils.findFilter;
import static java.util.Optional.ofNullable;

/**
 * Context for facet computation (statistics and filtering) containing data needed globally inside facet computation scope.
 *
 * @author Lukáš Hornych 2021
 */
@Getter
@Setter
public class FacetComputationContext {

    private static final StringTypedValueSerializer STRING_TYPED_VALUE_SERIALIZER = StringTypedValueSerializer.getInstance();

    private final EntityCollectionContext collectionCtx;
    private final FilterBy filterBy;

    private final Set<GroupReference> conjugatedGroups;
    private final Set<GroupReference> disjugatedGroups;
    private final Set<GroupReference> negatedGroups;

    private AllPossibleFacets allPossibleFacets;
    private List<FacetCondition> facetConditions;
    private int userFilterCount;

    public FacetComputationContext(@Nonnull EntityCollectionContext collectionCtx,
                                   @Nonnull List<FacetGroupsConjunction> facetGroupsConjunction,
                                   @Nonnull List<FacetGroupsDisjunction> facetGroupsDisjunction,
                                   @Nonnull List<FacetGroupsNegation> facetGroupsNegation,
                                   @Nullable FilterBy filterBy) {
        this.collectionCtx = collectionCtx;
        this.filterBy = filterBy;

        conjugatedGroups = facetGroupsConjunction
                .stream()
                .flatMap(it -> {
                    final String serializedEntityType = STRING_TYPED_VALUE_SERIALIZER.serialize(it.getEntityType()).getSerializedValue();
                    if (ArrayUtils.isEmpty(it.getFacetGroups())) {
                        return Stream.of(new GroupReference(it.getEntityType(), serializedEntityType, null));
                    } else {
                        return Arrays.stream(it.getFacetGroups())
                                .mapToObj(x -> new GroupReference(it.getEntityType(), serializedEntityType, x));
                    }
                })
                .collect(Collectors.toSet());
        disjugatedGroups = facetGroupsDisjunction
                .stream()
                .flatMap(it -> {
                    final String serializedEntityType = STRING_TYPED_VALUE_SERIALIZER.serialize(it.getEntityType()).getSerializedValue();
                    if (ArrayUtils.isEmpty(it.getFacetGroups())) {
                        return Stream.of(new GroupReference(it.getEntityType(), serializedEntityType, null));
                    } else {
                        return Arrays.stream(it.getFacetGroups())
                                .mapToObj(x -> new GroupReference(it.getEntityType(), serializedEntityType, x));
                    }
                })
                .collect(Collectors.toSet());
        negatedGroups = facetGroupsNegation
                .stream()
                .flatMap(it -> {
                    final String serializedEntityType = STRING_TYPED_VALUE_SERIALIZER.serialize(it.getEntityType()).getSerializedValue();
                    if (ArrayUtils.isEmpty(it.getFacetGroups())) {
                        return Stream.of(new GroupReference(it.getEntityType(), serializedEntityType, null));
                    } else {
                        return Arrays.stream(it.getFacetGroups())
                                .mapToObj(x -> new GroupReference(it.getEntityType(), serializedEntityType, x));
                    }
                })
                .collect(Collectors.toSet());
    }

    public boolean wasFacetRequested(@Nonnull FacetReference facet) {
        return ofNullable(filterBy)
                .map(fb -> {
                    final Predicate<FilterConstraint> predicate = fc -> fc instanceof Facet &&
                            Objects.equals(((Facet) fc).getEntityType(), facet.getEntityType()) &&
                            ArrayUtils.contains(((Facet) fc).getFacetIds(), facet.getFacetId());
                    return findFilter(fb, predicate) != null;
                })
                .orElse(false);
    }

    public GroupReference getGroupForFacet(FacetReference facet) {
        return allPossibleFacets.getFacetToGroupMap().get(facet);
    }

    public void recalculateFacetCountsInNegatedGroups(int baselineEntityCount) {
        allPossibleFacets.getGroupedAllPossibleFacets().entrySet()
                .stream()
                .filter(it -> negatedGroups.contains(it.getKey()))
                .forEach(it ->
                        // invert the results
                        it.getValue().forEach(facet -> facet.setCount(baselineEntityCount - facet.getCount()))
                );
    }
}
