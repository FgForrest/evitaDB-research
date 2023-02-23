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

package io.evitadb.query.indexSelection;

import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.filter.*;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.attribute.ReferenceHavingAttributeTranslator;
import io.evitadb.query.filter.translator.hierarchy.WithinHierarchyTranslator;
import io.evitadb.query.filter.translator.hierarchy.WithinRootHierarchyTranslator;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * This visitor examines {@link Query#getFilterBy()} constraint and tries to construct multiple {@link TargetIndexes}
 * alternative that can be used to fully interpret the filtering constraint. These alternative index sets will compete
 * one with another to produce filtering constraint with minimal execution costs.
 *
 * Currently, the logic is quite stupid - it searches the filter for all constraints within AND relation and when
 * relation or hierarchy constraint is encountered, it adds specific {@link EntityIndexType#REFERENCED_ENTITY} or
 * {@link EntityIndexType#REFERENCED_HIERARCHY_NODE} that contains limited subset of the entities related to that
 * placement/relation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class IndexSelectionVisitor implements ConstraintVisitor<FilterConstraint> {
	private final QueryContext queryContext;
	@Getter private final List<TargetIndexes> targetIndexes = new LinkedList<>();
	@Getter private boolean targetIndexQueriedByOtherConstraints;
	private FilterByVisitor filterByVisitor;
	private Serializable requestedEntityType;

	public IndexSelectionVisitor(@Nonnull QueryContext queryContext) {
		this.queryContext = queryContext;
		final EntityIndex globalIndex = queryContext.getEntityIndex(new EntityIndexKey(EntityIndexType.GLOBAL));
		if (globalIndex != null) {
			this.targetIndexes.add(
				new TargetIndexes(
					EntityIndexType.GLOBAL.name(),
					Collections.singletonList(
						globalIndex
					)
				)
			);
		}
	}

	@Override
	public void visit(@Nonnull FilterConstraint constraint) {
		if (constraint instanceof And || constraint instanceof FilterBy) {
			// if constraint is AND constraint we may traverse further
			@SuppressWarnings("unchecked") final FilterConstraint[] subConstraints = ((ConstraintContainer<FilterConstraint>) constraint).getConstraints();
			for (FilterConstraint subConstraint : subConstraints) {
				subConstraint.accept(this);
			}
		} else if (constraint instanceof HierarchyFilterConstraint) {
			// if constraint is hierarchy filtering constraint targeting different entity
			addHierarchyIndexOption(((HierarchyFilterConstraint) constraint));
		} else if (constraint instanceof ReferenceHavingAttribute) {
			// if constraint is hierarchy filtering constraint targeting different entity
			final ReferenceHavingAttribute referenceHavingAttribute = (ReferenceHavingAttribute) constraint;
			addReferenceIndexOption(referenceHavingAttribute);
			for (FilterConstraint subConstraint : referenceHavingAttribute.getConstraints()) {
				subConstraint.accept(this);
			}
		} else if (constraint instanceof IndexUsingConstraint) {
			this.targetIndexQueriedByOtherConstraints = true;
		}
	}

	/**
	 * Registers {@link TargetIndexes} that represents hierarchy placement. It finds collection of
	 * {@link EntityIndexType#REFERENCED_HIERARCHY_NODE} indexes that contains all relevant data for entities that
	 * are part of the requested tree. This significantly limits the scope that needs to be examined.
	 */
	private void addHierarchyIndexOption(@Nonnull HierarchyFilterConstraint constraint) {
		final Serializable requestedEntity = getRequestedEntityType();
		final Serializable filteredHierarchyEntity = constraint.getEntityType();
		if (filteredHierarchyEntity != null && !requestedEntity.equals(filteredHierarchyEntity)) {
			final EntityIndex targetHierarchyIndex = queryContext.getEntityIndex(
				filteredHierarchyEntity, new EntityIndexKey(EntityIndexType.GLOBAL)
			);
			if (targetHierarchyIndex == null) {
				// if target entity has no global index present, it means that the constraint cannot be fulfilled
				// we may quickly return empty result
				targetIndexes.add(TargetIndexes.EMPTY);
			} else {
				final int[] requestedHierarchyNodes;
				if (constraint instanceof WithinRootHierarchy) {
					final WithinRootHierarchy withinRootHierarchy = (WithinRootHierarchy) constraint;
					requestedHierarchyNodes = WithinRootHierarchyTranslator.createFormulaFromHierarchyIndexForDifferentEntity(
						withinRootHierarchy.getExcludedChildrenIds(),
						withinRootHierarchy.isDirectRelation(),
						targetHierarchyIndex
					);
				} else if (constraint instanceof WithinHierarchy) {
					final WithinHierarchy withinHierarchy = (WithinHierarchy) constraint;
					requestedHierarchyNodes = WithinHierarchyTranslator.createFormulaFromHierarchyIndexForDifferentEntity(
						withinHierarchy.getParentId(),
						withinHierarchy.getExcludedChildrenIds(),
						withinHierarchy.isDirectRelation(),
						withinHierarchy.isExcludingRoot(),
						targetHierarchyIndex
					);
				} else {
					//sanity check only
					throw new IllegalStateException("Should never happen");
				}
				// locate all hierarchy indexes
				final List<EntityIndex> theTargetIndexes = new ArrayList<>(requestedHierarchyNodes.length);
				for (Integer hierarchyEntityId : requestedHierarchyNodes) {
					ofNullable(
						queryContext.getEntityIndex(
							new EntityIndexKey(
								EntityIndexType.REFERENCED_HIERARCHY_NODE,
								new EntityReference(filteredHierarchyEntity, hierarchyEntityId)
							)
						)
					).ifPresent(theTargetIndexes::add);
				}
				// add indexes as potential target indexes
				this.targetIndexes.add(
					new TargetIndexes(
						EntityIndexType.REFERENCED_HIERARCHY_NODE.name() +
							" composed of " + requestedHierarchyNodes.length + " indexes",
						constraint,
						theTargetIndexes
					)
				);
			}
		}
	}

	/**
	 * Registers {@link TargetIndexes} that represents hierarchy placement. It finds collection of
	 * {@link EntityIndexType#REFERENCED_ENTITY} indexes that contains all relevant data for entities that
	 * are related to respective entity type and id. This may significantly limit the scope that needs to be examined.
	 */
	private void addReferenceIndexOption(@Nonnull ReferenceHavingAttribute constraint) {
		final List<EntityIndex> theTargetIndexes = ReferenceHavingAttributeTranslator.computeReferencedRecordIds(
			getFilterByVisitor(), constraint
		);

		// add indexes as potential target indexes
		this.targetIndexes.add(
			new TargetIndexes(
				EntityIndexType.REFERENCED_ENTITY.name() +
					" composed of " + theTargetIndexes.size() + " indexes",
				constraint,
				theTargetIndexes
			)
		);
	}

	private Serializable getRequestedEntityType() {
		if (requestedEntityType == null) {
			requestedEntityType = queryContext.getSchema().getName();
		}
		return requestedEntityType;
	}

	private FilterByVisitor getFilterByVisitor() {
		if (filterByVisitor == null) {
			// create lightweight visitor that can evaluate referenced attributes index location only
			filterByVisitor = new FilterByVisitor(
				this.queryContext,
				Collections.emptyList(),
				TargetIndexes.EMPTY,
				false
			);
		}
		return filterByVisitor;
	}

}
