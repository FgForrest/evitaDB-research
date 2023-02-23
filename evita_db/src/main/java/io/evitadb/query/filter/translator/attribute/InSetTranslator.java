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

import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.query.filter.InSet;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.index.bitmap.ArrayBitmap;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.attribute.AttributeFormula;
import io.evitadb.query.algebra.base.ConstantFormula;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.filter.FilterByVisitor;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * This implementation of {@link FilteringConstraintTranslator} converts {@link InSet} to {@link AbstractFormula}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InSetTranslator implements FilteringConstraintTranslator<InSet> {

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	@Nonnull
	public Formula translate(@Nonnull InSet filterConstraint, @Nonnull FilterByVisitor filterByVisitor) {
		final String attributeName = filterConstraint.getAttributeName();
		final Comparable<?>[] comparedValues = filterConstraint.getSet();
		final AttributeSchema attributeDefinition = filterByVisitor.getAttributeSchema(attributeName);
		final List<Serializable> valueStream = Arrays.stream(comparedValues)
			.map(it -> EvitaDataTypes.toTargetType((Serializable) it, attributeDefinition.getType()))
			.collect(Collectors.toList());

		// if attribute is unique prefer O(1) hash map lookup over histogram
		if (attributeDefinition.isUnique()) {
			return new AttributeFormula(
				attributeName,
				filterByVisitor.applyStreamOnUniqueIndexes(
					attributeDefinition,
					index -> valueStream.stream().map(it ->
						ofNullable(index.getRecordIdByUniqueValue(it))
							.map(x -> (Formula) new ConstantFormula(new ArrayBitmap(x)))
							.orElse(EmptyFormula.INSTANCE)
					)
				)
			);
		} else {
			return new AttributeFormula(
				attributeName,
				filterByVisitor.applyStreamOnFilterIndexes(
					attributeDefinition,
					index -> valueStream.stream().map(it -> index.getRecordsEqualToFormula((Comparable) it))
				)
			);
		}
	}

}
