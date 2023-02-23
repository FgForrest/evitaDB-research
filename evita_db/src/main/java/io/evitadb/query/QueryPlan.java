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

package io.evitadb.query;

import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.io.*;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.extraResult.ExtraResultProducer;
import io.evitadb.query.response.QueryTelemetry;
import io.evitadb.query.response.QueryTelemetry.QueryPhase;
import io.evitadb.query.sort.Sorter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.evitadb.query.response.QueryTelemetry.QueryPhase.EXTRA_RESULT_ITEM_FABRICATION;

/**
 * Query plan contains the full recipe on how the query result is going to be computed. Final result can be acquired
 * by calling {@link #execute()} method.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public class QueryPlan {
	/**
	 * Reference to the query context that allows to access entity bodies, indexes, original request and much more.
	 */
	@Delegate private final QueryContext queryContext;
	/**
	 * Contains prepared filtering formula, that takes all {@link FilterConstraint} in {@link EvitaRequest} into
	 * an account and produces set of entity primary keys that match the filter in query.
	 */
	private final Formula filteringFormula;
	/**
	 * Contains prepared sorter implementation that takes output of the {@link #filteringFormula} and sorts the entity
	 * primary keys according to {@link OrderConstraint} in {@link EvitaRequest}.
	 */
	private final Sorter sorter;
	/**
	 * Contains collections of computational objects that produce {@link EvitaResponseExtraResult} DTOs in reaction
	 * to {@link RequireConstraint} that are part of the input {@link EvitaRequest}.
	 */
	private final Collection<ExtraResultProducer> extraResultProducers;

	/**
	 * This method will {@link Formula#compute()} the filtered result, applies ordering and cuts out the requested page.
	 * Method is expected to be called only once per request.
	 */
	public <S extends Serializable, T extends EvitaResponseBase<S>> T execute() {
		final QueryTelemetry execution = queryContext.addStep(QueryPhase.EXECUTION);

		final QueryTelemetry filterExecution = execution.addStep(QueryPhase.EXECUTION_FILTER);
		// this call triggers the filtering computation and cause memoization of results
		final int totalRecordCount = filteringFormula.compute().size();
		filterExecution.finish();

		final QueryTelemetry sortAndSliceExecution = execution.addStep(QueryPhase.EXECUTION_SORT_AND_SLICE);
		final DataChunk<Integer> primaryKeys = queryContext.createDataChunk(totalRecordCount, filteringFormula, sorter);
		sortAndSliceExecution.finish();

		final T result;
		final EvitaRequest evitaRequest = queryContext.getEvitaRequest();
		final EntitySchema currentSchema = getSchema();
		// if full entity bodies are requested
		if (evitaRequest.isRequiresEntityBody()) {
			final QueryTelemetry fetching = execution.addStep(QueryPhase.FETCHING);

			// transform PKs to rich SealedEntities
			final DataChunk<SealedEntity> dataChunk = evitaRequest.createDataChunk(
				primaryKeys.getTotalRecordCount(),
				primaryKeys
					.stream()
					.map(queryContext::fetchEntity)
					.filter(Objects::nonNull)
					.collect(Collectors.toList())
			);

			// this may produce ClassCast exception if client assigns variable to different result than requests
			//noinspection unchecked
			result = (T) new EvitaEntityResponse(
				evitaRequest.getQuery(),
				dataChunk,
				// fabricate extra results
				fabricateExtraResults(dataChunk)
			);

			fetching.finish();
		} else {
			// this may produce ClassCast exception if client assigns variable to different result than requests
			final DataChunk<EntityReference> dataChunk = evitaRequest.createDataChunk(
				primaryKeys.getTotalRecordCount(),
				primaryKeys.stream()
					// returns simple reference to the entity (i.e. primary key and type of the entity)
					.map(pk -> new EntityReference(currentSchema.getName(), pk))
					.collect(Collectors.toList())
			);

			// this may produce ClassCast exception if client assigns variable to different result than requests
			//noinspection unchecked
			result = (T) new EvitaEntityReferenceResponse(
				evitaRequest.getQuery(),
				dataChunk,
				// fabricate extra results
				fabricateExtraResults(dataChunk)
			);
		}

		execution.finish();
		return result;
	}

	/**
	 * This method will process all {@link #extraResultProducers} and asks each an every of them to create an extra
	 * result that was requested in the query. Result array is not cached and execution cost is paid for each method
	 * call. This method is expected to be called only once, though.
	 */
	public EvitaResponseExtraResult[] fabricateExtraResults(@Nonnull DataChunk<? extends Serializable> dataChunk) {
		final LinkedList<EvitaResponseExtraResult> extraResults = new LinkedList<>();
		if (!extraResultProducers.isEmpty()) {
			final QueryTelemetry extraResultExecution = queryContext.addStep(QueryPhase.EXTRA_RESULTS_FABRICATION);
			for (ExtraResultProducer extraResultProducer : extraResultProducers) {
				// register sub-step for each fabricator so that we can track which were the costly ones
				final QueryTelemetry singleStep = extraResultExecution.addStep(
					EXTRA_RESULT_ITEM_FABRICATION,
					extraResultProducer.getClass().getSimpleName()
				);
				final EvitaResponseExtraResult extraResult = extraResultProducer.fabricate(dataChunk.getData());
				if (extraResult != null) {
					extraResults.add(extraResult);
				}
				singleStep.finish();
			}
			extraResultExecution.finish();
		}

		final QueryTelemetry telemetry = queryContext.getTelemetry();
		telemetry.finish();
		extraResults.add(telemetry);
		return extraResults.toArray(EvitaResponseExtraResult[]::new);
	}

}
