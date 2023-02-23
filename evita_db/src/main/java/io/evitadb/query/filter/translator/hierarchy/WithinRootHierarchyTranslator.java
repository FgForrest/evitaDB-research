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

package io.evitadb.query.filter.translator.hierarchy;

import io.evitadb.api.query.filter.WithinRootHierarchy;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.base.NotFormula;
import io.evitadb.query.algebra.infra.SkipFormula;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.indexSelection.TargetIndexes;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link WithinRootHierarchy} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class WithinRootHierarchyTranslator extends AbstractHierarchyTranslator<WithinRootHierarchy> {

	public static int[] createFormulaFromHierarchyIndexForDifferentEntity(int[] excludedChildrenIds, boolean directRelation, EntityIndex globalIndex) {
		if (directRelation) {
			if (ArrayUtils.isEmpty(excludedChildrenIds)) {
				return globalIndex.getRootHierarchyNodes().getArray();
			} else {
				return new NotFormula(
					new BaseBitmap(excludedChildrenIds),
					globalIndex.getRootHierarchyNodes()
				)
					.compute()
					.getArray();
			}
		} else {
			return globalIndex.listHierarchyNodesFromRoot(excludedChildrenIds).getArray();
		}
	}

	@Nonnull
	@Override
	public Formula translate(@Nonnull WithinRootHierarchy withinRootHierarchy, @Nonnull FilterByVisitor filterByVisitor) {
		final Serializable entityType = withinRootHierarchy.getEntityType();
		final int[] excludedChildrenIds = withinRootHierarchy.getExcludedChildrenIds();
		final boolean directRelation = withinRootHierarchy.isDirectRelation();

		if (entityType == null || entityType.equals(filterByVisitor.getSchema().getName())) {
			final EntityIndex globalIndex = filterByVisitor.getGlobalEntityIndex();
			return createFormulaFromHierarchyIndex(excludedChildrenIds, directRelation, globalIndex);
		} else {
			if (filterByVisitor.isTargetIndexRepresentingConstraint(withinRootHierarchy) && filterByVisitor.isTargetIndexQueriedByOtherConstraints()) {
				return SkipFormula.INSTANCE;
			} else {
				final TargetIndexes targetIndexes = filterByVisitor.findTargetIndexSet(withinRootHierarchy);
				if (targetIndexes == null) {
					final EntityIndex foreignEntityIndex = filterByVisitor.getGlobalEntityIndex(entityType);
					final int[] referencedIds = createFormulaFromHierarchyIndexForDifferentEntity(excludedChildrenIds, directRelation, foreignEntityIndex);
					return getReferencedEntityFormulas(filterByVisitor, entityType, referencedIds);
				} else {
					return getReferencedEntityFormulas(targetIndexes.getIndexes());
				}
			}
		}
	}

	private Formula createFormulaFromHierarchyIndex(int[] excludedChildrenIds, boolean directRelation, EntityIndex globalIndex) {
		if (directRelation) {
			if (ArrayUtils.isEmpty(excludedChildrenIds)) {
				return globalIndex.getRootHierarchyNodesFormula();
			} else {
				return new NotFormula(
					new ConstantFormula(
						new BaseBitmap(excludedChildrenIds)
					),
					globalIndex.getRootHierarchyNodesFormula()
				);
			}
		} else {
			return globalIndex.getListHierarchyNodesFromRootFormula(excludedChildrenIds);
		}
	}

}
