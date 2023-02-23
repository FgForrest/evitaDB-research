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

package io.evitadb.query.extraResult.translator.hierarchyStatistics;

import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.query.filter.WithinHierarchy;
import io.evitadb.api.query.require.HierarchyStatistics;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.extraResult.ExtraResultPlanningVisitor;
import io.evitadb.query.extraResult.ExtraResultProducer;
import io.evitadb.query.extraResult.translator.RequireConstraintTranslator;
import io.evitadb.query.extraResult.translator.hierarchyStatistics.producer.HierarchyStatisticsProducer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * This implementation of {@link RequireConstraintTranslator} converts {@link HierarchyStatistics} to
 * {@link HierarchyStatisticsProducer}. The producer instance has all pointer necessary to compute result.
 * All operations in this translator are relatively cheap comparing to final result computation, that is deferred to
 * {@link ExtraResultProducer#fabricate(DataChunk)} method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class HierarchyStatisticsTranslator implements RequireConstraintTranslator<HierarchyStatistics>, SelfTraversingTranslator {

	@Nonnull
	private static EntityIndexKey createReferencedHierarchyIndexKey(@Nonnull Serializable entityType, int hierarchyNodeId) {
		return new EntityIndexKey(EntityIndexType.REFERENCED_HIERARCHY_NODE, new EntityReference(entityType, hierarchyNodeId));
	}

	@Nonnull
	private static HierarchyStatisticsProducer getHierarchyStatisticsProducer(
		@Nonnull ExtraResultPlanningVisitor extraResultPlanner,
		@Nonnull Serializable queriedEntity,
		@Nullable Locale language,
		@Nonnull Formula filteringFormula
	) {
		return ofNullable(extraResultPlanner.findExistingProducer(HierarchyStatisticsProducer.class))
			.orElseGet(() -> new HierarchyStatisticsProducer(extraResultPlanner.getQueryContext(), queriedEntity, language, filteringFormula));
	}

	@Override
	public ExtraResultProducer apply(HierarchyStatistics hierarchyStatsConstraint, ExtraResultPlanningVisitor extraResultPlanner) {
		final Serializable queriedEntityType = extraResultPlanner.getSchema().getName();
		// target hierarchy type is either passed in constraint, or is the queried entity itself
		final Serializable entityType = ofNullable(hierarchyStatsConstraint.getEntityType())
			.orElse(queriedEntityType);

		// verify that requested entityType is hierarchical
		final EntitySchema entitySchema = extraResultPlanner.getSchema(entityType);
		Assert.isTrue(entitySchema.isWithHierarchy(), "Entity schema for `" + entitySchema.getName() + "` doesn't allow hierarchy!");

		// prepare shared data from the context
		final EvitaRequest evitaRequest = extraResultPlanner.getEvitaRequest();
		final Serializable queriedEntity = evitaRequest.getEntityType();
		final Locale language = evitaRequest.getLanguage();
		final WithinHierarchy withinHierarchy = evitaRequest.getWithinHierarchy(entityType);
		final EntityIndex globalIndex = extraResultPlanner.getGlobalEntityIndex(entityType);

		// retrieve existing producer or create new one
		final HierarchyStatisticsProducer hierarchyStatisticsProducer = getHierarchyStatisticsProducer(
			extraResultPlanner, queriedEntity, language, extraResultPlanner.getFilteringFormula()
		);

		// if we target the same entity as queried
		if (Objects.equals(entityType, queriedEntityType)) {
			// the request is simple - we use global index of current entity
			hierarchyStatisticsProducer
				.addHierarchyRequest(
					entityType,
					withinHierarchy,
					globalIndex,
					globalIndex::getHierarchyNodesForParent,
					hierarchyStatsConstraint.getRequirements()
				);
			return hierarchyStatisticsProducer;
		} else {
			// the request is more comples
			hierarchyStatisticsProducer
				.addHierarchyRequest(
					entityType,
					withinHierarchy,
					globalIndex,
					// we need to access EntityIndexType.REFERENCED_HIERARCHY_NODE of the queried type to access
					// entity primary keys that are referencing the hierarchy entity
					hierarchyNodeId -> ofNullable(extraResultPlanner.getEntityIndex(queriedEntityType, createReferencedHierarchyIndexKey(entityType, hierarchyNodeId)))
						.map(EntityIndex::getAllPrimaryKeys)
						.orElse(EmptyBitmap.INSTANCE),
					hierarchyStatsConstraint.getRequirements()
				);
			return hierarchyStatisticsProducer;
		}
	}

}
