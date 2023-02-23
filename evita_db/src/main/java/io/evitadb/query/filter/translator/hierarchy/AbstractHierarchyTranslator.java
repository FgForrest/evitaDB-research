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

import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Abstract super class for hierarchy constraint translators containing the shared logic.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
abstract class AbstractHierarchyTranslator<T extends FilterConstraint> implements FilteringConstraintTranslator<T>, SelfTraversingTranslator {

	/**
	 * Method returns {@link Formula} that returns all entity ids that are referencing ids in `pivotIds`.
	 */
	@Nonnull
	protected Formula getReferencedEntityFormulas(@Nonnull FilterByVisitor filterByVisitor, @Nonnull Serializable entityType, @Nonnull int[] pivotIds) {
		// hierarchy indexes are tied to the entity that is being requested
		final Serializable containingEntityType = filterByVisitor.getSchema().getName();
		// return OR product of all indexed primary keys in those indexes
		return FormulaFactory.or(
			Arrays.stream(pivotIds)
				// for each pivot id create EntityIndexKey to REFERENCED_HIERARCHY_NODE entity index
				.mapToObj(referencedId -> new EntityIndexKey(
					EntityIndexType.REFERENCED_HIERARCHY_NODE,
					new EntityReference(entityType, referencedId)
				))
				// get the index
				.map(it -> filterByVisitor.getEntityIndex(containingEntityType, it))
				// filter out indexes that are not present (no entity references the pivot id)
				.filter(Objects::nonNull)
				// get all entity ids referencing the pivot id
				.map(EntityIndex::getAllPrimaryKeysFormula)
				// filter out empty formulas (with no results) to optimize computation
				.filter(it -> !(it instanceof EmptyFormula))
				// return as array
				.toArray(Formula[]::new)
		);
	}

	/**
	 * Method returns {@link Formula} that returns all entity ids that are referencing ids in `pivotIds`.
	 */
	@Nonnull
	protected Formula getReferencedEntityFormulas(@Nonnull List<EntityIndex> entityIndexes) {
		// return OR product of all indexed primary keys in those indexes
		return FormulaFactory.or(
			entityIndexes.stream()
				// get all entity ids referencing the pivot id
				.map(EntityIndex::getAllPrimaryKeysFormula)
				// filter out empty formulas (with no results) to optimize computation
				.filter(it -> !(it instanceof EmptyFormula))
				// return as array
				.toArray(Formula[]::new)
		);
	}

}
