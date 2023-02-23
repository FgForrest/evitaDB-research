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

package io.evitadb.query.filter.translator.attribute;

import io.evitadb.api.query.filter.PrimaryKey;
import io.evitadb.api.query.filter.ReferenceHavingAttribute;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.base.OrFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.indexSelection.TargetIndexes;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.Assert.notNull;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link PrimaryKey} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ReferenceHavingAttributeTranslator implements FilteringConstraintTranslator<ReferenceHavingAttribute>, SelfTraversingTranslator {
	private static final Formula[] EMPTY_FORMULA = new Formula[0];

	public static List<EntityIndex> computeReferencedRecordIds(@Nonnull FilterByVisitor filterByVisitor, @Nonnull ReferenceHavingAttribute filterConstraint) {
		final Serializable referencedType = filterConstraint.getEntityType();
		final EntityIndex entityIndex = filterByVisitor.getEntityIndex(new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY_TYPE, referencedType));
		if (entityIndex == null) {
			return Collections.emptyList();
		}

		final boolean referencesHierarchicalEntity = filterByVisitor.isReferencingHierarchicalEntity(referencedType);
		final ReferenceSchema referenceSchema = filterByVisitor.getSchema().getReferenceOrThrowException(referencedType);
		final int[] referencedRecordIds = computeReferencedRecordIds(referenceSchema::getAttribute, filterConstraint, filterByVisitor, entityIndex);
		return Arrays.stream(referencedRecordIds)
			.mapToObj(it -> filterByVisitor.getReferencedEntityIndex(referencedType, referencesHierarchicalEntity, it))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Formula translate(@Nonnull ReferenceHavingAttribute filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final Serializable referencedType = filterConstraint.getEntityType();
		final EntitySchema entitySchema = filterByVisitor.getSchema();
		final ReferenceSchema referenceSchema = entitySchema.getReference(referencedType);
		notNull(referenceSchema, "Reference of type `" + referencedType + "` is not present in schema of `" + entitySchema.getName() + "` entity.");
		isTrue(referenceSchema.isIndexed(), "Reference of type `" + referencedType + "` is indexed. Sorting by attributes without index would be slow.");

		final List<EntityIndex> referencedEntityIndexes = getTargetIndexes(filterConstraint, filterByVisitor, referencedType, referenceSchema);
		if (referencedEntityIndexes.isEmpty()) {
			return EmptyFormula.INSTANCE;
		} else {
			return applySearchOnIndexes(filterConstraint, filterByVisitor, referenceSchema, referencedEntityIndexes);
		}
	}

	private Formula applySearchOnIndexes(@Nonnull ReferenceHavingAttribute filterConstraint, @Nonnull FilterByVisitor filterByVisitor, ReferenceSchema referenceSchema, List<EntityIndex> referencedEntityIndexes) {
		final List<Formula> referencedEntityFormulas = new ArrayList<>(referencedEntityIndexes.size());
		for (EntityIndex referencedEntityIndex : referencedEntityIndexes) {
			referencedEntityFormulas.add(
				filterByVisitor.executeInContext(
					Collections.singletonList(referencedEntityIndex),
					referenceSchema::getAttribute,
					() -> {
						filterConstraint.getConstraint().accept(filterByVisitor);
						final Formula[] collectedFormulas = filterByVisitor.getCollectedIntegerFormulasOnCurrentLevel();
						switch (collectedFormulas.length) {
							case 0:
								return filterByVisitor.getSuperSetFormula();
							case 1:
								return collectedFormulas[0];
							default:
								return new OrFormula(collectedFormulas);
						}
					},
					PrimaryKey.class
				)
			);
		}
		return FormulaFactory.or(
			referencedEntityFormulas.toArray(EMPTY_FORMULA)
		);
	}

	private List<EntityIndex> getTargetIndexes(@Nonnull ReferenceHavingAttribute filterConstraint, @Nonnull FilterByVisitor filterByVisitor, Serializable referencedType, ReferenceSchema referenceSchema) {
		final TargetIndexes targetIndexes = filterByVisitor.findTargetIndexSet(filterConstraint);
		final List<EntityIndex> referencedEntityIndexes;
		if (targetIndexes == null) {
			final EntityIndex entityIndex = filterByVisitor.getEntityIndex(new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY_TYPE, referencedType));
			if (entityIndex == null) {
				referencedEntityIndexes = Collections.emptyList();
			} else {
				final int[] referencedRecordIds = computeReferencedRecordIds(referenceSchema::getAttribute, filterConstraint, filterByVisitor, entityIndex);
				final boolean referencesHierarchicalEntity = filterByVisitor.isReferencingHierarchicalEntity(referencedType);
				referencedEntityIndexes = Arrays.stream(referencedRecordIds)
					.mapToObj(it -> filterByVisitor.getReferencedEntityIndex(referencedType, referencesHierarchicalEntity, it))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			}
		} else {
			referencedEntityIndexes = targetIndexes.getIndexes();
		}
		return referencedEntityIndexes;
	}

	private static int[] computeReferencedRecordIds(@Nonnull Function<String, AttributeSchema> attributeSchemaAccessor, @Nonnull ReferenceHavingAttribute filterConstraint, @Nonnull FilterByVisitor filterByVisitor, @Nonnull EntityIndex entityIndex) {
		return filterByVisitor.executeInContext(
				Collections.singletonList(entityIndex),
				attributeSchemaAccessor,
				() -> {
					filterConstraint.getConstraint().accept(filterByVisitor);
					final Formula[] collectedFormulas = filterByVisitor.getCollectedIntegerFormulasOnCurrentLevel();
					filterByVisitor.resetCollectedFormulasOnCurrentLevel();
					isTrue(collectedFormulas.length == 1, "Exactly one formula is expected to be produced!");
					return collectedFormulas[0];
				}
			)
			.compute()
			.getArray();
	}

}
