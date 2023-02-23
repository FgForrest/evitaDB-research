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

package io.evitadb.query.sort.attribute.translator;

import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.order.ReferenceAttribute;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.sort.OrderByVisitor;
import io.evitadb.query.sort.Sorter;
import io.evitadb.query.sort.translator.OrderingConstraintTranslator;

import java.io.Serializable;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.Assert.notNull;

/**
 * This implementation of {@link OrderingConstraintTranslator} converts {@link ReferenceAttribute} to {@link Sorter}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ReferenceAttributeTranslator implements OrderingConstraintTranslator<ReferenceAttribute>, SelfTraversingTranslator {

	@Override
	public Sorter apply(ReferenceAttribute orderConstraint, OrderByVisitor orderByVisitor) {
		final Serializable referencedType = orderConstraint.getEntityType();
		final EntitySchema entitySchema = orderByVisitor.getSchema();
		final ReferenceSchema referenceSchema = entitySchema.getReference(referencedType);
		notNull(referenceSchema, "Reference of type `" + referencedType + "` is not present in schema of `" + entitySchema.getName() + "` entity.");
		isTrue(referenceSchema.isIndexed(), "Reference of type `" + referencedType + "` is indexed. Sorting by attributes without index would be slow.");

		return orderByVisitor.executeInContext(
			new EntityIndexKey(EntityIndexType.REFERENCED_ENTITY_TYPE, referencedType),
			referenceSchema::getAttribute,
			() -> {
				for (OrderConstraint innerConstraint : orderConstraint.getConstraints()) {
					innerConstraint.accept(orderByVisitor);
				}
				return orderByVisitor.getLastUsedSorter();
			}
		);
	}

}
