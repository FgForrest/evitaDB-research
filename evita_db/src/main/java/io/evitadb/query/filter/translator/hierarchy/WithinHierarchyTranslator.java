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

import io.evitadb.api.query.filter.WithinHierarchy;
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
 * This implementation of {@link FilteringConstraintTranslator} converts {@link WithinHierarchy} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class WithinHierarchyTranslator extends AbstractHierarchyTranslator<WithinHierarchy> {

	public static int[] createFormulaFromHierarchyIndexForDifferentEntity(int parentId, int[] excludedChildrenIds, boolean directRelation, boolean excludingRoot, EntityIndex globalIndex) {
		if (directRelation) {
			if (ArrayUtils.isEmpty(excludedChildrenIds)) {
				return new int[]{parentId};
			} else {
				throw new IllegalArgumentException("Excluded constraint has no sense when direct relation constraint is used as well!");
			}
		} else {
			return excludingRoot ?
				globalIndex.listHierarchyNodesFromParent(parentId, excludedChildrenIds).getArray() :
				globalIndex.listHierarchyNodesFromParentIncludingItself(parentId, excludedChildrenIds).getArray();
		}
	}

	@Nonnull
	@Override
	public Formula translate(@Nonnull WithinHierarchy withinHierarchy, @Nonnull FilterByVisitor filterByVisitor) {
		final Serializable entityType = withinHierarchy.getEntityType();
		final int parentId = withinHierarchy.getParentId();
		final int[] excludedChildrenIds = withinHierarchy.getExcludedChildrenIds();
		final boolean directRelation = withinHierarchy.isDirectRelation();
		final boolean excludingRoot = withinHierarchy.isExcludingRoot();

		if (entityType == null || entityType.equals(filterByVisitor.getSchema().getName())) {
			final EntityIndex globalIndex = filterByVisitor.getGlobalEntityIndex();
			return createFormulaFromHierarchyIndexForSameEntity(
				parentId, excludedChildrenIds, directRelation, excludingRoot, globalIndex
			);
		} else {
			if (filterByVisitor.isTargetIndexRepresentingConstraint(withinHierarchy) && filterByVisitor.isTargetIndexQueriedByOtherConstraints()) {
				return SkipFormula.INSTANCE;
			} else {
				final TargetIndexes targetIndexes = filterByVisitor.findTargetIndexSet(withinHierarchy);
				if (targetIndexes == null) {
					final EntityIndex foreignEntityIndex = filterByVisitor.getGlobalEntityIndex(entityType);
					final int[] referencedIds = createFormulaFromHierarchyIndexForDifferentEntity(
						parentId, excludedChildrenIds, directRelation, excludingRoot, foreignEntityIndex
					);
					return getReferencedEntityFormulas(filterByVisitor, entityType, referencedIds);

				} else {
					return getReferencedEntityFormulas(targetIndexes.getIndexes());
				}
			}
		}
	}

	private Formula createFormulaFromHierarchyIndexForSameEntity(int parentId, int[] excludedChildrenIds, boolean directRelation, boolean excludingRoot, EntityIndex globalIndex) {
		if (directRelation) {
			if (ArrayUtils.isEmpty(excludedChildrenIds)) {
				return globalIndex.getHierarchyNodesForParentFormula(parentId);
			} else {
				return new NotFormula(
					new ConstantFormula(
						new BaseBitmap(excludedChildrenIds)
					),
					globalIndex.getHierarchyNodesForParentFormula(parentId)
				);
			}
		} else {
			return excludingRoot ?
				globalIndex.getListHierarchyNodesFromParentFormula(parentId, excludedChildrenIds) :
				globalIndex.getListHierarchyNodesFromParentIncludingItselfFormula(parentId, excludedChildrenIds);
		}
	}

}
