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

package io.evitadb.query.extraResult.translator.histogram;

import io.evitadb.api.io.extraResult.Histogram;
import io.evitadb.api.query.require.AttributeHistogram;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.attribute.FilterIndex;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.attribute.AttributeFormula;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder;
import io.evitadb.query.algebra.utils.visitor.FormulaFinder.LookUp;
import io.evitadb.query.extraResult.ExtraResultPlanningVisitor;
import io.evitadb.query.extraResult.ExtraResultProducer;
import io.evitadb.query.extraResult.translator.RequireConstraintTranslator;
import io.evitadb.query.extraResult.translator.histogram.producer.AttributeHistogramProducer;
import io.evitadb.query.indexSelection.TargetIndexes;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This implementation of {@link RequireConstraintTranslator} converts {@link AttributeHistogram} to {@link Histogram}.
 * The producer instance has all pointer necessary to compute result. All operations in this translator are relatively
 * cheap comparing to final result computation, that is deferred to {@link ExtraResultProducer#fabricate(List)}
 * method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class AttributeHistogramTranslator implements RequireConstraintTranslator<AttributeHistogram> {

	@Override
	public ExtraResultProducer apply(AttributeHistogram attributeHistogram, ExtraResultPlanningVisitor extraResultPlanner) {
		// initialize basic data necessary for th computation
		final Locale language = extraResultPlanner.getEvitaRequest().getLanguage();
		final EntitySchema schema = extraResultPlanner.getSchema();
		final String[] attributeNames = attributeHistogram.getAttributeNames();
		final int bucketCount = attributeHistogram.getRequestedBucketCount();

		// find user filters that enclose variable user defined part
		final Set<Formula> userFilters = extraResultPlanner.getUserFilteringFormula();
		// in them find all AttributeFormulas and create index for them
		final Map<String, List<AttributeFormula>> attributeFormulas = userFilters.stream()
			.flatMap(it -> FormulaFinder.find(it, AttributeFormula.class, LookUp.SHALLOW).stream())
			.collect(Collectors.groupingBy(AttributeFormula::getAttributeName));

		// get all indexes that should be used for query execution
		final TargetIndexes indexSetToUse = extraResultPlanner.getIndexSetToUse();
		// find existing AttributeHistogramProducer for potential reuse
		AttributeHistogramProducer attributeHistogramProducer = extraResultPlanner.findExistingProducer(AttributeHistogramProducer.class);
		for (String attributeName : attributeNames) {
			// if there was no producer ready, create new one
			if (attributeHistogramProducer == null) {
				attributeHistogramProducer = new AttributeHistogramProducer(
					schema.getName(), extraResultPlanner.getQueryContext(),
					bucketCount,
					extraResultPlanner.getFilteringFormula(),
					extraResultPlanner.getCacheSupervisor()
				);
			}

			// collect all FilterIndexes for requested attribute and requested language
			final List<FilterIndex> attributeIndexes = indexSetToUse.getIndexes()
				.stream()
				.map(it -> it.getFilterIndex(attributeName, language))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

			// retrieve attribute schema for requested attribute
			final AttributeSchema attributeSchema = getAttributeSchema(schema, attributeName);

			// register computational lambda for producing attribute histogram
			attributeHistogramProducer.addAttributeHistogramRequest(
				attributeSchema, attributeIndexes, attributeFormulas.get(attributeName)
			);
		}
		return attributeHistogramProducer;
	}

	@Nonnull
	private AttributeSchema getAttributeSchema(EntitySchema schema, String attributeName) {
		final AttributeSchema attributeSchema = schema.getAttribute(attributeName);
		Assert.isTrue(
			attributeSchema != null,
			"Attribute `" + attributeName + "` was not found in the schema of entity `" + schema.getName() + "`!"
		);
		Assert.isTrue(
			Number.class.isAssignableFrom(attributeSchema.getType()),
			"Attribute `" + attributeName + "` must be a number in order to compute histogram!"
		);
		return attributeSchema;
	}

}
