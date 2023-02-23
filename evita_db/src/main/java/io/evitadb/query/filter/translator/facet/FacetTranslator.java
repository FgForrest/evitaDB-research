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

package io.evitadb.query.filter.translator.facet;

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.And;
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.query.filter.Not;
import io.evitadb.api.query.filter.Or;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.query.QueryExecutor.FutureNotFormula;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.base.NotFormula;
import io.evitadb.query.algebra.facet.CombinedFacetFormula;
import io.evitadb.query.algebra.facet.FacetGroupAndFormula;
import io.evitadb.query.algebra.facet.FacetGroupFormula;
import io.evitadb.query.algebra.facet.FacetGroupOrFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.evitadb.api.utils.Assert.isTrue;
import static java.util.Optional.ofNullable;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link io.evitadb.api.query.filter.Facet} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FacetTranslator implements FilteringConstraintTranslator<Facet> {

	@Nonnull
	@Override
	public Formula translate(@Nonnull Facet filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		return filterByVisitor.applyOnIndexes(
			entityIndex -> {
				// first collect all formulas
				final Collection<FacetGroupFormula> facetReferencingEntityIdsFormula = entityIndex.getFacetReferencingEntityIdsFormula(
					filterConstraint.getEntityType(),
					(groupId, facetIds, recordIdBitmaps) -> {
						final ReferenceSchema referenceSchema = filterByVisitor.getSchema().getReferenceOrThrowException(filterConstraint.getEntityType());
						isTrue(referenceSchema.isFaceted(), "Reference of type `" + filterConstraint.getEntityType() + "` is faceted.");
						if (referenceSchema.isGroupTypeRelatesToEntity() && filterByVisitor.isFacetGroupConjunction(referenceSchema.getGroupType(), groupId)) {
							// AND relation is requested for facet of this group
							return new FacetGroupAndFormula(
								filterConstraint.getEntityType(), groupId, facetIds, recordIdBitmaps
							);
						} else {
							// default facet relation inside same group is or
							return new FacetGroupOrFormula(
								filterConstraint.getEntityType(), groupId, facetIds, recordIdBitmaps
							);
						}
					},
					filterConstraint.getFacetIds()
				);

				// no single entity references this particular facet - return empty result quickly
				if (facetReferencingEntityIdsFormula.isEmpty()) {
					return EmptyFormula.INSTANCE;
				}

				// now aggregate formulas by their group relation type
				final Map<Class<? extends FilterConstraint>, List<FacetGroupFormula>> formulasGroupedByAggregationType = facetReferencingEntityIdsFormula
					.stream()
					.collect(
						Collectors.groupingBy(
							it -> {
								final Integer groupId = it.getFacetGroupId();
								if (groupId != null) {
									final ReferenceSchema referenceSchema = filterByVisitor.getSchema().getReferenceOrThrowException(filterConstraint.getEntityType());
									if (referenceSchema.isGroupTypeRelatesToEntity()) {
										// OR relation is requested for facets of this group
										if (filterByVisitor.isFacetGroupDisjunction(referenceSchema.getGroupType(), groupId)) {
											return Or.class;
											// NOT relation is requested for facets of this group
										} else if (filterByVisitor.isFacetGroupNegation(referenceSchema.getGroupType(), groupId)) {
											return Not.class;
										}
									}
								}
								// default group relation is and
								return And.class;
							}
						)
					);

				// wrap formulas to appropriate containers
				final Formula notFormula = ofNullable(formulasGroupedByAggregationType.get(Not.class))
					.map(it -> FormulaFactory.or(it.toArray(Formula[]::new)))
					.orElse(null);
				final Formula andFormula = ofNullable(formulasGroupedByAggregationType.get(And.class))
					.map(it -> FormulaFactory.and(it.toArray(Formula[]::new)))
					.orElse(null);
				final Formula orFormula = ofNullable(formulasGroupedByAggregationType.get(Or.class))
					.map(it -> FormulaFactory.or(it.toArray(Formula[]::new)))
					.orElse(null);

				if (notFormula == null) {
					if (andFormula == null && orFormula == null) {
						throw new IllegalArgumentException("This should be not possible!");
					} else if (andFormula == null) {
						return orFormula;
					} else if (orFormula == null) {
						return andFormula;
					} else if (orFormula instanceof FacetGroupFormula) {
						return new CombinedFacetFormula(andFormula, orFormula);
					} else {
						return orFormula.getCloneWithInnerFormulas(
							ArrayUtils.insertRecordIntoArray(andFormula, orFormula.getInnerFormulas(), orFormula.getInnerFormulas().length)
						);
					}
				} else {
					if (andFormula == null && orFormula == null) {
						return new FutureNotFormula(notFormula);
					} else if (andFormula == null) {
						return new NotFormula(notFormula, orFormula);
					} else if (orFormula == null) {
						return new NotFormula(notFormula, andFormula);
					} else {
						return new NotFormula(
							notFormula,
							new CombinedFacetFormula(andFormula, orFormula)
						);
					}
				}
			}
		);
	}

}
