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

package io.evitadb.query.extraResult.translator.parents;

import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.query.require.Parents;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.extraResult.ExtraResultPlanningVisitor;
import io.evitadb.query.extraResult.ExtraResultProducer;
import io.evitadb.query.extraResult.translator.RequireConstraintTranslator;
import io.evitadb.query.extraResult.translator.parents.producer.ParentsProducer;

import java.io.Serializable;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * This implementation of {@link RequireConstraintTranslator} converts {@link Parents} to {@link ParentsProducer}.
 * The producer instance has all pointer necessary to compute result. All operations in this translator are relatively
 * cheap comparing to final result computation, that is deferred to {@link ExtraResultProducer#fabricate(DataChunk)} method.
 *
 * This translator interoperates with {@link ParentsOfTypeTranslator} and shares/reuses same {@link ParentsProducer}
 * instance.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class ParentsTranslator implements RequireConstraintTranslator<Parents>, SelfTraversingTranslator {

	@Override
	public ExtraResultProducer apply(Parents parentConstraint, ExtraResultPlanningVisitor extraResultPlanner) {
		// verify that requested entityType is hierarchical
		final EntitySchema entitySchema = extraResultPlanner.getSchema();
		Assert.isTrue(entitySchema.isWithHierarchy(), "Entity schema for `" + entitySchema.getName() + "` doesn't allow hierarchy!");

		final EvitaRequest evitaRequest = extraResultPlanner.getEvitaRequest();
		final Set<Serializable> entityReferenceSet = evitaRequest.getEntityReferenceSet();
		final BiFunction<Integer, Serializable, Integer[]> referenceFetcher = (entityPrimaryKey, referencedEntityType) ->
			extraResultPlanner.getReferencesStorageContainer(entityPrimaryKey).getReferencedIds(referencedEntityType);

		// find existing ParentsProducer for potential reuse
		final ParentsProducer existingParentsProducer = extraResultPlanner.findExistingProducer(ParentsProducer.class);
		if (existingParentsProducer == null) {
			// if no ParentsProducer exists yet - create new one
			return new ParentsProducer(
				extraResultPlanner.getQueryContext(),
				entityType -> evitaRequest.isRequiresEntityReferences() && entityReferenceSet.isEmpty() || entityReferenceSet.contains(entityType),
				entitySchema.getName(),
				false,
				referenceFetcher,
				extraResultPlanner.getGlobalEntityIndex(entitySchema.getName()),
				parentConstraint.getRequirements()
			);
		} else {
			// otherwise, just add another computational lambda
			existingParentsProducer.addRequestedParents(
				entitySchema.getName(),
				extraResultPlanner.getGlobalEntityIndex(entitySchema.getName()),
				parentConstraint.getRequirements()
			);
			return existingParentsProducer;
		}
	}

}
